import torch
import torch.nn as nn
import torch.optim as optim
import helper_utils
import numpy
import random
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore
import fantasy_calculator
from fantasy_calculator import set_doubles, to_numpy_array, calc_fantasy

sample_idx = 21705
fantasy_weights = fantasy_calculator.fantasy_weights_all
checkpoint_path = "best_model.pt"
torch.manual_seed(42)

if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  statsStore = StatsStore(PlayerStore())
  stats = statsStore.get_stats(2017, False, set_doubles)
  print("total stats", len(stats))
  print(stats[sample_idx])
  val_stats = random.choices(statsStore.get_stats(2016, False, set_doubles), k=20000)
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
  model = nn.Sequential(
      nn.Linear(in_dims, hidden),
      nn.ReLU(),
      # nn.Linear(hidden, hidden),
      # nn.ReLU(),
      nn.Linear(hidden, out_dims))
  model = nn.Sequential(nn.Linear(in_dims, out_dims))
  loss_function = nn.MSELoss()
  optimizer = optim.Adam(model.parameters(), lr=0.1)
  best_val_loss = float('inf')  # Keeps track of the best validation loss so far
  for epoch in range(2500):
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

  # helper_utils.plot_data(model, train_x, train_y)

  layer = model[0]
  result_weights = numpy.transpose(layer.weight.data.numpy())
  bias = layer.bias.data.numpy()
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], result_weights[i])
  model_create_time = time.time()

  print(stats[sample_idx])
  print(train_y[sample_idx])
  with torch.no_grad():
    checkpoint = torch.load("best_model.pt")
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
    epoch = checkpoint['epoch']
    best_val_loss = checkpoint['val_loss']
    # Pass the new data to the trained model to get a prediction
    predicted_time = model(train_x[sample_idx])

    # Use .item() to extract the scalar value from the tensor for printing
    print(f"Prediction {predicted_time}")
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "transform time:", transform_time - load_time,
        "model Creation time:", model_create_time - transform_time,
        'verify time:', end_time - model_create_time)