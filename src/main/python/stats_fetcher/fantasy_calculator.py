import numpy as np
# import mlx.core as np
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore


fantasy_weights = { # FanDuel, DraftKings, NBA and NBA2017 Fantasy weights
  'player_game_num_career': [0.0, 0.0, 0.0, 0.0],
  'team_game_num_season': [0.0, 0.0, 0.0, 0.0],
  'fg': [2.0, 2.0, 2.0, 2.0],
  # 'fga': [0.0, 0.0, 0.0, 0.0],
  # 'fg_pct': [0.0, 0.0, 0.0, 0.0],
  'fg3': [1.0, 1.5, 1.0, 1.0],
  # 'fg3a': [0.0, 0.0, 0.0, 0.0],
  # 'fg3_pct': [0.0, 0.0, 0.0, 0.0],
  # 'fg2': [0.0, 0.0, 0.0, 0.0],
  # 'fg2a': [0.0, 0.0, 0.0, 0.0],
  # 'fg2_pct': [0.0, 0.0, 0.0, 0.0],
  # 'efg_pct': [0.0, 0.0, 0.0, 0.0],
  'ft': [1.0, 1.0, 1.0, 1.0],
  # 'fta': [0.0, 0.0, 0.0, 0.0],
  # 'ft_pct': [0.0, 0.0, 0.0, 0.0],
  # 'orb': [0.0, 0.0, 0.0, 0.0],
  # 'drb': [0.0, 0.0, 0.0, 0.0],
  'trb': [1.2, 1.25, 1.0, 1.2],
  'ast': [1.5, 1.5, 2.0, 1.5],
  'stl': [3.0, 2.0, 3.0, 3.0],
  'blk': [3.0, 2.0, 3.0, 3.0],
  'tov': [-1.0, -0.5, -1.0, -1.0],
  'pf': [0.0, 0.0, 0.0, 0.0],
  # 'pts': [0.0, 0.0, 0.0, 0.0],
  'game_score': [0.0, 0.0, 0.0, 0.0],
  'plus_minus': [0.0, 0.0, 0.0, 0.0],
  'double_double': [0.0, 1.5, 0.0, 0.0],
  'triple_double': [0.0, 3.0, 0.0, 0.0],
}
double_fields = ['pts', 'trb', 'ast', 'stl', 'blk']
sample_idx = 959020


def get_doubles(stat: dict):
  doubles = 0
  for df in double_fields:
    stat[df] = stat.get(df) if stat.get(df) is not None else 0.0
    if stat[df] >= 10:
      doubles += 1
  return doubles


def set_doubles(stat: dict):
  doubles = get_doubles(stat)
  stat['double_double'] = stat['triple_double'] = 0
  if doubles == 2:
    stat['double_double'] = 1
  if doubles > 2:
    stat['triple_double'] = 1
  return stat


def to_numpy_array(stats: list):
  fan_values = []
  keys = list(fantasy_weights.keys())
  for stat in stats:
    value_list = [stat.get(key, 0.0) for key in keys]
    value_list = [0.0 if x is None else x for x in value_list]
    fan_values.append(value_list)
  return np.array(fan_values, dtype=np.float32)


def calc_fantasy(npa):
  weights = np.array(list(fantasy_weights.values()), dtype=np.float32)
  return np.matmul(npa, weights)

if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  statsStore = StatsStore(PlayerStore())
  stats = []
  loaded = []
  for year in range(1947, 2026):
    loaded.extend(statsStore.get_stats(year, False, set_doubles))
  for x in range(1):
    stats.extend(loaded)
  print("total stats", len(stats))
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  npa = to_numpy_array(stats)
  for fw in list(fantasy_weights.keys()):
    for x in range(0):
      fantasy_weights[fw].extend([1, 2, 3, 4, 5, 6, 7, 8, 10])
  print(npa[sample_idx])
  transform_time = time.time()

  # Perform the matrix multiplication to calculate the fantasy scores.
  results = calc_fantasy(npa)
  print(results[sample_idx])
  matmul_time = time.time()

  for i in range(len(stats)):
   stat = stats[i]
   stat['fanduel'] = results[i][0].item()
   stat['draftkings'] = results[i][1].item()
   stat['nba'] = results[i][2].item()
   stat['nba2017'] = results[i][3].item()

  sorted_stats = sorted(stats, key=lambda s:s['draftkings'], reverse=True)
  for i in range(50):
    print(i + 1, sorted_stats[i])
  end_time = time.time()

  mmt = matmul_time - transform_time
  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time, "transform time:",
        transform_time - load_time, "matmul time:", mmt,
        'set & sort time:', end_time - matmul_time)
  fpos = (len(npa) * len(list(fantasy_weights.values())) * len(fantasy_weights['fg']))
  print("Billion Floating Point Operations:", fpos / 1000000000, "GFLOPs:", (1 / mmt * fpos) / 1000000000)