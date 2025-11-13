import keras
import numpy
import random
import time
from nba_stats_store import StatsStore
from nba_player_store import PlayerStore
from fantasy_calculator import set_doubles, to_numpy_array, fantasy_weights, calc_fantasy
from keras.callbacks import ModelCheckpoint, EarlyStopping

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
  train_x = to_numpy_array(stats)
  print(train_x[sample_idx])
  val_x = to_numpy_array(random.choices(stats, k=512))
  weights = numpy.array(list(fantasy_weights.values()), dtype=numpy.float32)
  transform_time = time.time()

  train_y = calc_fantasy(train_x)
  val_y = calc_fantasy(val_x)
  model = keras.Sequential(
      [keras.layers.Dense(input_shape=[len(weights)], units=len(weights[0]))])
  model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')
  checkpoint = ModelCheckpoint(
      filepath='fantasy_model.keras',
      monitor='val_loss',  # Metric to monitor (e.g., 'val_accuracy', 'loss')
      verbose=1,           # Level of verbosity (0: silent, 1: progress bar, 2: one line per epoch)
      save_best_only=True, # Save only when the monitored metric improves
      mode='min'           # 'min' for metrics like loss, 'max' for metrics like accuracy
  )
  early_stopping = EarlyStopping(
      monitor='val_loss',  # Metric to monitor (e.g., validation loss)
      patience=16,         # Number of epochs with no improvement after which training will be stopped
      mode='min',          # 'min' for metrics that should decrease (like loss), 'max' for metrics that should increase (like accuracy)
      restore_best_weights=True # Restore model weights from the epoch with the best monitored value
  )
  model.fit(train_x, train_y, epochs=500, batch_size=256,
            validation_data=(val_x, val_y),
            callbacks=[checkpoint, early_stopping])
  weights = model.get_weights()[0]
  for i in range(len(fantasy_weights.keys())):
    print(list(fantasy_weights.keys())[i], list(fantasy_weights.values())[i], weights[i])
  model_create_time = time.time()

  print(stats[sample_idx])
  print(train_y[sample_idx])
  print(f'Predict: {model.predict(numpy.array([train_x[sample_idx]]))}')
  # model.predict(npa)
  end_time = time.time()

  print("Total time:", end_time - start_time,
        "\n\tLoad time:", load_time - start_time,
        "transform time:", transform_time - load_time,
        "model Creation time:", model_create_time - transform_time,
        'verify time:', end_time - model_create_time)