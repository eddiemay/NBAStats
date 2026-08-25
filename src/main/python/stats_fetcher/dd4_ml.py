import torch
import torch.nn as nn
from torch.utils.data import Dataset

class DD4PyTorchModel(nn.Module):
  def __init__(self, in_features=None, out_features=None, hidden_features=None,
      loss_function=nn.CrossEntropyLoss(), layers=None,
      checkpoint_path=None):
    super().__init__()
    self.device = get_best_device()

    self.loss_function = loss_function
    self.flatten = nn.Flatten()
    self.layers = layers if layers else nn.Sequential(
        nn.Linear(in_features, 16),
        nn.ReLU(),
        nn.Linear(16, 8),
        nn.ReLU(),
        nn.Linear(8, out_features),
        # nn.Softmax()
    )
    self.to(self.device)
    self.best_val_accuracy = 0
    self.checkpoint_path = checkpoint_path

  def forward(self, x):
    return self.layers(self.flatten(x))

  def train_epoch(self, train_x, train_y, epoch, optimizer):
    self.train()
    total = len(train_x)

    optimizer.zero_grad()
    output = self(train_x)
    loss = self.loss_function(output, train_y)
    loss.backward()
    optimizer.step()

    # Track progress
    _, predicted = output.max(1)
    correct = predicted.eq(train_y).sum().item()

    # Print every 100 batches
    if epoch % 100 == 0:
      accuracy = 100. * correct / total
      print(f' [{epoch} Loss: {loss.item():.3f} | Accuracy: {accuracy:.1f}%')

  def evaluate(self, val_x, val_y):
    self.eval()

    with torch.no_grad():
      outputs = self(val_x)
      _, predicted = outputs.max(1)
      correct = predicted.eq(val_y).sum().item()
      val_loss = self.loss_function(outputs, val_y).item()

    return 100. * correct / len(val_x), val_loss

  def train_model(self, train_x, train_y, val_x, val_y, optimizer, epochs):
    # Training loop
    best_val_accuracy = 0
    for epoch in range(1, epochs + 1):
      print('\nEpoch: ', epoch)
      self.train_epoch(train_x, train_y, epoch, optimizer)
      accuracy, val_loss = self.evaluate(val_x, val_y)
      print(f' Validation Loss: {val_loss:.3f} | Accuracy: {accuracy:.2f}%')
      # --- Checkpoint ---
      if self.checkpoint_path and accuracy > best_val_accuracy:
        print(f" Validation improved from {best_val_accuracy:.2f}% → {accuracy:.2f}%. Saving model.")
        best_val_accuracy = accuracy
        torch.save({
          'epoch': epoch,
          'model_state_dict': self.state_dict(),
          'optimizer_state_dict': optimizer.state_dict(),
          'val_accuracy': accuracy
        }, self.checkpoint_path)


class DD4Subset(Dataset):
  def __init__(self, subset, transform):
    self.subset = subset
    self.transform = transform

  # How many total samples
  def __len__(self):
    return len(self.subset)

  # How to get image and label number 'idx'
  def __getitem__(self, idx):
    image, label = self.subset[idx]
    if self.transform:
      image = self.transform(image)
    return image, label

def random_split(dataset, lengths, transforms):
  splits = torch.utils.data.random_split(dataset, lengths)
  subsets = []
  for split, transform in zip(splits, transforms):
    subsets.append(DD4Subset(split, transform))
  return subsets

def get_best_device():
  if torch.cuda.is_available():
    device = torch.device('cuda')
  elif torch.backends.mps.is_available():
    device = torch.device('mps')
  else:
    device = torch.device('cpu')
  print('using device:', device)
  return device