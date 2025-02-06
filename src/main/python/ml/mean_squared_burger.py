# Find the pricing of Mean Squared Burger Shop
# Burgers cost $1.99, Fries cost $1.49 and Drinks cost $0.98
import numpy as np
from tensorflow import keras

model = keras.Sequential([keras.layers.Dense(units=1, input_shape=[3])])

model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')

orders = np.array(
    [[2.0, 1.0, 1.0], [2.0, 2.0, 2.0], [5.0, 3.0, 5.0], [7.0, 4.0, 4.0],
     [3.0, 0.0, 0.0], [3.0, 2.0, 0.0], [2.0, 4.0, 3.0]], dtype=float)
totals = np.array([6.45, 8.92, 19.32, 23.81, 5.97, 8.95, 12.88], dtype=float)

model.fit(orders, totals, epochs=500)

# Should print $44.60 = 10 x 1.99 + 10 x 1.49 + 10 x .98
print(model.predict(np.array([[10.0, 10.0, 10.0]])))