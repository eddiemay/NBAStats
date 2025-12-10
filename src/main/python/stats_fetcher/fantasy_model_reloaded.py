import keras
import mlx.core as mx
import time
import torch
from fantasy_calculator import fantasy_weights, set_doubles, to_numpy_array, matmul_fantasy
from fantasy_model_maker_mlx import FantasyModelMLX
from fantasy_model_maker_pytorch import FantasyModelPytorch
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore

sample_idx = 21705

if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  statsStore = StatsStore(PlayerStore())
  stats = []
  for year in range(1947, 2026):
    stats.extend(statsStore.get_stats(year, False, set_doubles))
  print("total stats", len(stats))
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  npa = to_numpy_array(stats, fantasy_weights)
  print(npa[sample_idx])
  transform_time = time.time()

  # Reload the Keras model and predict.
  model = keras.models.load_model('fantasy_model.keras')
  results = model.predict(npa)
  print('Keras prediction:', results[sample_idx])
  keras_time = time.time()

  # Reload the Pytorch model and predict.
  model = FantasyModelPytorch(npa.shape[1], 4)
  model.load_state_dict(torch.load("fantasy_model.pt", map_location="mps")['model_state_dict'])
  model.eval()
  # Run inference
  with torch.no_grad():
    results = model(torch.tensor(npa))
    print('PyTorch prediction:', results[sample_idx])
  pytorch_time = time.time()

  # Reload the MLX model and predict.
  model = FantasyModelMLX(npa.shape[1], 4)
  model.load_weights('fantasy_model.safetensors')
  results = model(mx.array(npa))
  print('MLX prediction:', results[sample_idx])
  mlx_time = time.time()

  # Run the matmul time.
  results = matmul_fantasy(npa, fantasy_weights)
  print('Actual result:', results[sample_idx])
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time, "transform time:",
        transform_time - load_time,
        "Keras time:", keras_time - transform_time,
        "PyTorch time:", pytorch_time - keras_time,
        "MLX time:", mlx_time - pytorch_time,
        "matmul time:", end_time - mlx_time)
