import keras
import numpy
# import mlx.core as mlx
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore
from fantasy_calculator import set_doubles, to_numpy_array, fantasy_weights, sample_idx


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
  npa = to_numpy_array(stats)
  print(npa[sample_idx])
  transform_time = time.time()

  # Perform the matrix multiplication to calculate the fantasy scores.
  model = keras.models.load_model('fantasy_model.keras')
  weights = model.get_weights()[0]
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], weights[i])
  results = model.predict(npa)
  print(results[sample_idx])
  matmul_time = time.time()

  for i in range(len(stats)):
    stat = stats[i]
    stat['fanduel'] = results[i][0].item()
    stat['draftkings'] = results[i][1].item()
    stat['nba'] = results[i][2].item()
    stat['nba2017'] = results[i][3].item()

  sorted_stats = sorted(stats, key=lambda s:s['nba'], reverse=True)
  for i in range(50):
    print((i + 1), sorted_stats[i])
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time, "transform time:",
        transform_time - load_time, "matmul time:", matmul_time - transform_time,
        'set & sort time:', end_time - matmul_time)
