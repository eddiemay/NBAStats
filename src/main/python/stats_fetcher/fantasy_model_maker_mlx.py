import keras
import mlx.core as mx
import mlx.optimizers as optim
import time
from fantasy_calculator import calc_fantasy, fantasy_weights, load_training_data, to_numpy_array
from mlx import nn
from mlx.nn.losses import mse_loss

sample_idx = 21705

# 1. Define the model
class FantasyModelMLX(nn.Module):
  def __init__(self, in_dims: int, out_dims: int, hidden_dims: int = 64):
    super().__init__()
    self.layer = nn.Sequential(nn.Linear(in_dims, out_dims))

  def __call__(self, x):
    return self.layer(x)

# 2️⃣ Define mean squared error loss
def loss_fn(model, x_mx, y_mx):
  preds = model(x_mx)
  return mx.mean((preds - y_mx) ** 2)


if __name__ == '__main__':
  start_time = time.time()

  # Load the data
  stats, val_stats = load_training_data()
  print(stats[sample_idx])
  load_time = time.time()

  # Transform the data from dict array to numpy array
  train_x = mx.array(to_numpy_array(stats, fantasy_weights))
  print(train_x[sample_idx])
  val_x = mx.array(to_numpy_array(val_stats, fantasy_weights))
  print('train_x shape: ', train_x.shape)
  transform_time = time.time()

  train_y = mx.array(calc_fantasy(stats))
  val_y = mx.array(calc_fantasy(val_stats))
  model = FantasyModelMLX(in_dims=train_x.shape[1], out_dims=train_y.shape[1])
  mx.eval(model.parameters())
  optimizer = optim.Adam(learning_rate=0.1)
  loss_and_grad_fn = nn.value_and_grad(model, loss_fn)
  best_val_loss = float('inf')
  # 3. Training loop
  for epoch in range(10000):
    loss, grads = loss_and_grad_fn(model, train_x, train_y)
      # mx.eval(loss, grads)
    optimizer.update(model, grads)
    mx.eval(model.parameters(), optimizer.state)
    if (epoch + 1) % 50 == 0:
      print("epoch:", epoch + 1, "loss:", loss.item())
      val_loss = loss_fn(model, val_x, val_y)
      if val_loss < best_val_loss:
        print(f"Validation improved from {best_val_loss:.6f} → {loss:.6f}. Saving model.")
        best_val_loss = val_loss
        model.save_weights("fantasy_model.safetensors")
  result_weights = mx.transpose(model.layer.layers[0].weight)
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], result_weights[i])
  model_create_time = time.time()

  print(stats[sample_idx])
  print(train_y[sample_idx])
  # params = mx.load("fantasy_model.safetensors")
  # model.update(params)
  print('Predict: ', model(mx.array([train_x[sample_idx]])))
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "Transform time:", transform_time - load_time,
        "Model time:", model_create_time - transform_time,
        'Verify time:', end_time - model_create_time)