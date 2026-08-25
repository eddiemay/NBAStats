import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import time
import torch
from fantasy_calculator import fantasy_weights_no_doubles, load_training_data, to_numpy_array, set_doubles
from mpl_toolkits.mplot3d import Axes3D
from nba_player_store import PlayerStore
from nba_stats_store import StatsStore
from torch import nn, optim
from dd4_ml import DD4PyTorchModel, get_best_device

fantasy_weights = fantasy_weights_no_doubles

sample_idx = 21705

pd.set_option("display.max_columns", None)
pd.set_option("display.width", None)

def doubles_only(stats):
  return np.array(stats['doubles'].to_numpy(dtype=np.float32)).T


def plot3D(df):
  fig = plt.figure()
  ax = fig.add_subplot(111, projection='3d')

  colors = [
    ["neither", "gray"],
    ["double", "blue"],
    ["triple", "red"]
  ]

  for i in range(len(colors)):
    label, color = colors[i]
    subset = df[df["pred_dbl"] == i]
    ax.scatter(
        subset["pts"],
        subset["ast"],
        subset["trb"],
        c=color,
        label=label,
        s=60
    )

  ax.set_xlabel("Points")
  ax.set_ylabel("Assists")
  ax.set_zlabel("Rebounds")
  ax.legend()

  plt.show()


def plot2D(df):
  plt.figure()

  colors = [
    ["neither", "gray"],
    ["double", "blue"],
    ["triple", "red"]
  ]

  for i in range(len(colors)):
    label, color = colors[i]
    subset = df[df["pred_dbl"] == i]
    plt.scatter(
        subset["pts"],
        subset["ast"],
        # subset["trb"],
        c=color,
        label=label,
    )

  plt.xlabel("Points")
  plt.ylabel("Assists")
  #plt.zlabel("Rebounds")
  plt.legend()

  plt.show()


if __name__ == '__main__':
  start_time = time.time()
  device = get_best_device()

  # Load the data
  stats, val_stats = load_training_data()
  print(stats.iloc[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  train_x = torch.tensor(to_numpy_array(stats, fantasy_weights)).to(device)
  print("train_x sample:", train_x[sample_idx])
  val_x = torch.tensor(to_numpy_array(val_stats, fantasy_weights)).to(device)
  transform_time = time.time()

  train_y = torch.tensor(doubles_only(stats)).to(device)
  print("train_y sample:", train_y[sample_idx])
  checkpoint_path = "fantasy_model_double.pt"
  val_y = torch.tensor(doubles_only(val_stats)).to(device)
  in_dims = train_x.shape[1]
  out_dims = 3
  hidden_dims = 12
  model = DD4PyTorchModel(
      in_dims, out_dims, hidden_dims, checkpoint_path=checkpoint_path).to(device)
  optimizer = optim.Adam(model.parameters(), lr=0.01)

  model.train_model(train_x, train_y, val_x, val_y, optimizer, 1000)

  layer = model.layers[0]
  result_weights = layer.weight.data.T # Transpose the weight data for display.
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], result_weights[i])
  model_create_time = time.time()

  statsStore = StatsStore(PlayerStore())
  stats2017 = statsStore.get_stats(2017, False, set_doubles)
  westbrook2017 = stats2017[stats2017['name'] == 'Russell Westbrook'][
    ['name', 'date'] + list(fantasy_weights.keys()) + ['doubles']
  ]
  westbrook_df = pd.concat([
    westbrook2017[westbrook2017['doubles'] == 0].head(14),
    westbrook2017[westbrook2017['doubles'] == 1].head(14),
    westbrook2017[westbrook2017['doubles'] == 2].head(14)], ignore_index=True)
  print(westbrook_df)
  print(doubles_only(westbrook_df))
  with torch.no_grad():
    checkpoint = torch.load(checkpoint_path)
    model.load_state_dict(checkpoint['model_state_dict'])
    optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
    epoch = checkpoint['epoch']
    best_val_accuracy = checkpoint['val_accuracy']
    # Pass the new data to the trained model to get a prediction
    predicted = model(torch.tensor(to_numpy_array(westbrook_df, fantasy_weights)).to(device))
    # Use .item() to extract the scalar value from the tensor for printing
    westbrook_df['pred_dbl'] = predicted.max(1)[1].cpu()
    print(f"Prediction: {westbrook_df['pred_dbl']}")
    plot3D(westbrook_df)
    plot2D(westbrook_df)
  verify_time = time.time()

  end_time = time.time()
  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "transform time:", transform_time - load_time,
        "model Creation time:", model_create_time - transform_time,
        'verify time:', verify_time - model_create_time,
        'export time:', end_time - verify_time)