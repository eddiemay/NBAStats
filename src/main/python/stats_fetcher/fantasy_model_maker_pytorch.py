import numpy
import random
import time
import torch
from fantasy_calculator import calc_fantasy, fantasy_weights, load_training_data, to_numpy_array
from torch import nn, optim

sample_idx = 21705
checkpoint_path = "fantasy_model.pt"
torch.manual_seed(42)

class FantasyModelPytorch(nn.Module):
  def __init__(self, in_dims: int, out_dims: int, hidden_dims: int = 64):
    super().__init__()
    self.layer = nn.Sequential(nn.Linear(in_dims, out_dims))

  def forward(self, x):
    return self.layer(x)


def export(trained_model, input_sample):
  print(input_sample.shape)
  trained_model.eval()
  torch.onnx.export(
      trained_model,
      input_sample,
      "fantasy_model_pt_export.onnx",
      export_params=True,
      opset_version=13,
      do_constant_folding=True,
      input_names=['stats'],
      output_names=['fantasy'],
      dynamic_axes={'stats': {0: 'batch_size'}, 'fantasy': {0: 'batch_size'}}
  )


if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  stats, val_stats = load_training_data()
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  train_x = torch.tensor(to_numpy_array(stats, fantasy_weights))
  print(train_x[sample_idx])
  val_x = torch.tensor(to_numpy_array(val_stats, fantasy_weights))
  transform_time = time.time()

  train_y = torch.tensor(calc_fantasy(stats))
  print(train_y[sample_idx])
  val_y = torch.tensor(calc_fantasy(val_stats))
  in_dims = train_x.shape[1]
  out_dims = train_y.shape[1]
  hidden = in_dims * 2
  model = FantasyModelPytorch(in_dims, out_dims)
  loss_function = nn.MSELoss()
  optimizer = optim.Adam(model.parameters(), lr=0.1)
  best_val_loss = float('inf')  # Keeps track of the best validation loss so far
  for epoch in range(10000):
    # Reset the optimizer's gradients
    optimizer.zero_grad()
    # Make predictions (forward pass)
    outputs = model(train_x)
    # Calculate the loss
    loss = loss_function(outputs, train_y)
    # Calculate adjustments (backward pass)
    loss.backward()
    # Update the model's parameters
    optimizer.step()
    # Print loss every 50 epochs
    if (epoch + 1) % 50 == 0:
      print(f"Epoch {epoch + 1}: Loss = {loss.item()}")
      val_loss = loss_function(model(val_x), val_y)
      # --- Checkpoint ---
      if val_loss < best_val_loss:
        print(f"Validation improved from {best_val_loss:.6f} → {loss:.6f}. Saving model.")
        best_val_loss = val_loss
        torch.save({
          'epoch': epoch,
          'model_state_dict': model.state_dict(),
          'optimizer_state_dict': optimizer.state_dict(),
          'val_loss': val_loss
        }, checkpoint_path)

  layer = model.layer[0]
  result_weights = numpy.transpose(layer.weight.data.numpy())
  bias = layer.bias.data.numpy()
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], result_weights[i])
  model_create_time = time.time()

  print(stats[sample_idx])
  print(train_y[sample_idx])
  with torch.no_grad():
    checkpoint = torch.load(checkpoint_path)
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
    epoch = checkpoint['epoch']
    best_val_loss = checkpoint['val_loss']
    # Pass the new data to the trained model to get a prediction
    predicted = model(train_x[sample_idx])

    # Use .item() to extract the scalar value from the tensor for printing
    print(f"Prediction {predicted}")
  verify_time = time.time()

  export(model, train_x[sample_idx].unsqueeze(0))
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "transform time:", transform_time - load_time,
        "model Creation time:", model_create_time - transform_time,
        'verify time:', verify_time - model_create_time,
        'export time:', end_time - verify_time)