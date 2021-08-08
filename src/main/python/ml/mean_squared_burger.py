import tensorflow as tf
import numpy as np
from tensorflow import keras

model = tf.keras.Sequential([keras.layers.Dense(units=1, input_shape=[3])])

model.compile(optimizer='sgd', loss='mean_squared_error')

orders = np.array([[2.0, 1.0, 1.0], [2.0, 2.0, 2.0], [5.0, 3.0, 5.0], [7.0, 4.0, 4.0], [3.0, 0.0, 0.0], [3.0, 2.0, 0.0]], dtype=float, dtype=float, dtype=float)
totals = np.array([5.46, 8.92, 19.37, 23.85, 5.97, 8.95], dtype=float)

model.fit(totals, orders, epochs=500)

print(model.predict([10.0, 10.0, 10.0]))