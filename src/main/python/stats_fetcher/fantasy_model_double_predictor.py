import numpy as np
import pandas as pd
import time
import torch
from fantasy_calculator import fantasy_weights_no_doubles, load_training_data, to_numpy_array, set_doubles
from nba_player_store import PlayerStore
from nba_stats_store import StatsStore
from torch import nn, optim
from fantasy_model_maker_pytorch import FantasyModelPytorch, get_best_device

fantasy_weights = fantasy_weights_no_doubles

sample_idx = 21705

def doubles_only(stats):
  stats_df = pd.DataFrame(stats)
  return np.array(stats_df['doubles'].to_numpy(dtype=np.float32)).T

if __name__ == '__main__':
  start_time = time.time()
  device = get_best_device()

  # Load the data
  stats, val_stats = load_training_data()
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  train_x = torch.tensor(to_numpy_array(stats, fantasy_weights)).to(device)
  print("train_x sample:", train_x[sample_idx])
  val_x = torch.tensor(to_numpy_array(val_stats, fantasy_weights)).to(device)
  transform_time = time.time()

  train_y = torch.tensor(doubles_only(stats)).to(device)
  print("train_y sample:", train_y[sample_idx])
  val_y = torch.tensor(doubles_only(val_stats)).to(device)
  in_dims = train_x.shape[1]
  out_dims = 1
  hidden_dims = 12
  model = FantasyModelPytorch(in_dims, out_dims, nn.MSELoss(), hidden_dims=hidden_dims).to(device)
  optimizer = optim.Adam(model.parameters(), lr=0.01)

  checkpoint_path = "fantasy_model_double.pt"
  model.train_model(train_x, train_y, val_x, val_y, optimizer, 1000, checkpoint_path=checkpoint_path)

  layer = model.layers[0]
  result_weights = layer.weight.data.T # Transpose the weight data for display.
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], result_weights[i])
  model_create_time = time.time()

  statsStore = StatsStore(PlayerStore())
  stats2017 = pd.DataFrame(statsStore.get_stats(2017, False, set_doubles))
  westbrook2017 = stats2017[stats2017['name'] == 'Russell Westbrook']
  westbrookND = westbrook2017[westbrook2017['doubles'] == 0].iloc[0].to_dict()
  westbrookDD = westbrook2017[westbrook2017['doubles'] == 1].iloc[0].to_dict()
  westbrookTD = westbrook2017[westbrook2017['doubles'] == 2].iloc[0].to_dict()
  print(westbrookND)
  print(westbrookDD)
  print(westbrookTD)
  print(doubles_only([westbrookND, westbrookDD, westbrookTD]))
  with torch.no_grad():
    checkpoint = torch.load(checkpoint_path)
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
    epoch = checkpoint['epoch']
    best_val_loss = checkpoint['val_loss']
    # Pass the new data to the trained model to get a prediction
    predicted = model(torch.tensor(
        to_numpy_array([westbrookND, westbrookDD, westbrookTD], fantasy_weights)).to(device))
    # Use .item() to extract the scalar value from the tensor for printing
    print(f"Prediction {predicted}")
  verify_time = time.time()

  end_time = time.time()
  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "transform time:", transform_time - load_time,
        "model Creation time:", model_create_time - transform_time,
        'verify time:', verify_time - model_create_time,
        'export time:', end_time - verify_time)