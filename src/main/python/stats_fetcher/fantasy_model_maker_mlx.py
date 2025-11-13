import keras
import mlx.core as mx
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore
from fantasy_calculator import set_doubles, to_numpy_array, fantasy_weights, calc_fantasy
import mlx.nn as nn
from mlx.nn.losses import mse_loss
import mlx.optimizers as optim
from mlx.utils import tree_flatten


# 1. Define the model
class MLP(nn.Module):
  def __init__(self, in_dims: int, out_dims: int, hidden_dims: int = 64):
    super().__init__()
    self.layer = nn.Linear(in_dims, out_dims)

  def __call__(self, x):
    return self.layer(x)

# 2️⃣ Define mean squared error loss
def mse_loss(pred, target):
  diff = pred - target
  return mx.mean(diff * diff)

# 2. Instantiate and define training
def loss_fn(model, x_mx, y_mx):
  preds = model(x_mx)
  return mx.mean((preds - y_mx) ** 2)

sample_idx = 1000

if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  statsStore = StatsStore(PlayerStore())
  stats = statsStore.get_stats(2017, False, set_doubles)
  print("total stats", len(stats))
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  npa = mx.array(to_numpy_array(stats))
  print(npa[sample_idx])
  weights = mx.array(list(fantasy_weights.values()), dtype=mx.float32)
  transform_time = time.time()

  results = calc_fantasy(npa)
  model = MLP(in_dims=len(weights), out_dims=len(weights[0]))
  mx.eval(model.parameters())
  optimizer = optim.Adam(learning_rate=0.1)
  loss_and_grad_fn = nn.value_and_grad(model, loss_fn)
  # 3. Training loop
  for epoch in range(2500):
    loss, grads = loss_and_grad_fn(model, npa, results)
      # mx.eval(loss, grads)
    optimizer.update(model, grads)
    mx.eval(model.parameters(), optimizer.state)
    if (epoch + 1) % 50 == 0:
      print("epoch:", epoch + 1, "loss:", loss.item())
  state = tree_flatten(optimizer.state, destination={})
  mx.save_safetensors("fantasy_model.safetensors", state)
  model.save_weights("fantasy_model_weights.safetensors")
  # weights = model.get_weights()[0]
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], weights[i])
  model_create_time = time.time()

  print(stats[sample_idx])
  print(results[sample_idx])
  print('Predict: ', model(mx.array([npa[sample_idx]])))
  # model.predict(npa)
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "Transform time:", transform_time - load_time,
        "Model time:", model_create_time - transform_time,
        'Verify time:', end_time - model_create_time)