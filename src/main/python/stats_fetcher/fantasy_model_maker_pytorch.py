import torch
import torch.nn as nn
import torch.optim as optim
import helper_utils
import numpy
import random
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore
from fantasy_calculator import set_doubles, to_numpy_array, fantasy_weights, calc_fantasy

sample_idx = 1000
# torch.manual_seed(42)

if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  statsStore = StatsStore(PlayerStore())
  stats = statsStore.get_stats(2017, False, set_doubles)
  print("total stats", len(stats))
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  train_x = torch.tensor(to_numpy_array(stats))
  print(train_x[sample_idx])
  val_x = torch.tensor(to_numpy_array(random.choices(stats, k=512)))
  weights = numpy.array(list(fantasy_weights.values()), dtype=numpy.float32)
  transform_time = time.time()

  train_y = calc_fantasy(train_x)
  print(train_y[sample_idx])
  val_y = calc_fantasy(val_x)
  model = nn.Sequential(nn.Linear(len(weights), len(weights[0])))
  loss_function = nn.MSELoss()
  optimizer = optim.Adam(model.parameters(), lr=0.1)
  for epoch in range(1500):
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

  # helper_utils.plot_data(model, train_x, train_y)

  layer = model[0]
  weights = layer.weight.data.numpy()
  bias = layer.bias.data.numpy()
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], model)
  model_create_time = time.time()

  print(stats[sample_idx])
  print(train_y[sample_idx])
  with torch.no_grad():
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