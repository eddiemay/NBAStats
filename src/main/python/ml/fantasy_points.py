# Finds how basketball scoring is done from inputs of:
# Minutes played, field goals made, field goal attempts, 3 point baskets made,
# 3 point attempts, free throws made and free throw attempts.
import tensorflow as tf
import numpy as np
from tensorflow import keras

model = tf.keras.Sequential([keras.layers.Dense(units=1, input_shape=[14])])
model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')

gamelog = np.array(
    [[38.8, 8.0, 18.0, 0.0, 3.0, 4.0, 10.0, 0.0, 2.0, 4.0, 0.0, 0.0, 3.4, 4.4],
     [35.0, 9.0, 22.0, 0.0, 0.0, 5.0, 7.0, 6.0, 17.0, 1.0, 1.0, 4.0, 3.0, 2.0],
     [39.0, 2.0, 13.0, 2.0, 8.0, 2.0, 2.0, 1.0, 4.0, 2.1, 1.0, 0.0, 1.0, 2.0],
     [26.0, 3.0, 7.0, 1.0, 4.0, 0.0, 0.0, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 5.0],
     [42.0, 28.0, 46.0, 7.0, 13.0, 18.0, 20.0, 2.0, 4.0, 2.0, 3.0, 1.0, 3.0, 1.0],
     [35.0, 5.0, 11.0, 1.0, 5.0, 2.0, 2.0, 1.0, 1.0, 4.0, 3.0, 0.0, 2.0, 5.0],
     [43.0, 1.0, 7.0, 1.0, 2.0, 5.0, 6.0, 0.0, 10.0, 7.0, 0.0, 2.0, 4.0, 3.0],
     [33.0, 18.0, 31.0, 4.0, 10.0, 22.0, 25.0, 3.0, 5.0, 0.0, 3.0, 0.0, 2.0, 3.0],
     [29.0, 2.0, 7.0, 1.0, 3.0, 2.0, 6.0, 0.0, 0.0, 2.0, 2.0, 0.0, 3.0, 2.0],
     [34.0, 2.0, 5.0, 1.0, 1.0, 2.0, 4.0, 0.0, 8.0, 3.0, 1.0, 2.0, 1.0, 2.0],
     [39.0, 21.0, 37.0, 3.0, 4.0, 10.0, 11.0, 0.0, 4.0, 2.0, 1.0, 0.0, 2.0, 3.0],
     [41.0, 7.0, 12.0, 1.0, 4.0, 4.0, 5.0, 0.0, 9.0, 8.0, 2.0, 0.0, 1.0, 4.0],
     [37.0, 8.0, 23.0, 2.0, 9.0, 2.0, 3.0, 5.0, 15.0, 21.0, 3.0, 0.0, 2.0, 2.0],
     [40.0, 18.0, 27.0, 2.0, 3.0, 6.0, 7.0, 2.0, 8.0, 4.0, 0.0, 3.0, 0.0, 2.0],
     [47.0, 21.0, 31.0, 2.0, 6.0, 16.0, 22.0, 5.0, 16.0, 10.0, 2.0, 1.0, 4.0, 5.0],
     [50.0, 22.0, 34.0, 7.0, 15.0, 20.0, 25.0, 3.0, 5.0, 11.0, 0.0, 1.0, 4.0, 3.0],
     [24.0, 7.0, 7.0, 5.0, 5.0, 5.0, 5.0, 0.0, 7.0, 6.0, 2.0, 0.0, 0.0, 4.0],
     [42.0, 22.0, 50.0, 6.0, 21.0, 10.0, 12.0, 0.0, 4.0, 4.0, 1.0, 1.0, 2.0, 1.0],
     [39.0, 9.0, 22.0, 1.0, 4.0, 6.0, 6.0, 4.0, 6.0, 2.0, 2.0, 2.0, 6.0, 4.0],
     [47.0, 9.0, 24.0, 1.0, 5.0, 8.0, 10.0, 1.0, 10.0, 11.0, 2.0, 3.0, 5.0, 1.0],
     [43.0, 10.0, 23.0, 2.0, 5.0, 4.0, 4.0, 3.0, 3.0, 1.0, 1.0, 1.0, 2.0, 3.0],
     [41.7, 11.7, 23.7, 1.9, 5.0, 4.4, 7.2, 2.4, 8.9, 8.9, 2.6, 2.3, 4.4, 2.3],
     [39.0, 10.2, 22.0, 2.1, 5.3, 4.4, 4.7, 1.1, 2.7, 3.9, 2.1, 0.7, 2.6, 2.6],
     [34.0, 10.0, 17.0, 1.0, 3.0, 7.0, 10.0, 0.0, 4.0, 6.0, 1.0, 1.0, 5.0, 3.0]],
    dtype=float)
points = np.array(
    [25.4, 56.9, 19.0, 22.1, 100.2, 28.4, 32.5, 78.6, 13.0, 29.1, 63.8, 46.8,
     82.5, 71.0, 105.2, 96.1, 47.4, 74.8, 46.0, 66.7, 38.7, 66.91, 43.31, 42.8],
    dtype=float)

model.fit(gamelog, points, epochs=500)
print('weights:', model.weights)
# Should print 59.7 fantasy points
print(
    model.predict(
        [[47.0, 9.0, 28.0, 0.0, 7.0, 6.0, 10.0, 6.0, 10.0, 9.0, 2.0, 0.0, 3.0, 0.0]]))