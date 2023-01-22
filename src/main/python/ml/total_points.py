# Finds how basketball scoring is done from inputs of:
# Minutes played, field goals made, field goal attempts, 3 point baskets made,
# 3 point attempts, free throws made and free throw attempts.
import tensorflow as tf
import numpy as np
from tensorflow import keras

model = tf.keras.Sequential([keras.layers.Dense(units=1, input_shape=[7])])
model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')

gamelog = np.array(
    [[30.0, 2.0, 11.0, 0.0, 7.0, 1.0, 1.0],
     [23.0, 4.0, 16.0, 1.0, 5.0, 2.0, 8.0],
     [45.0, 8.0, 8.0, 2.0, 2.0, 5.0, 6.0],
     [38.0, 10.0, 16.0, 5.0, 8.0, 11.0, 15.0],
     [42.0, 28.0, 46.0, 7.0, 13.0, 18.0, 20.0],
     [35.0, 5.0, 11.0, 1.0, 5.0, 2.0, 2.0],
     [43.0, 1.0, 7.0, 1.0, 2.0, 5.0, 6.0],
     [33.0, 18.0, 31.0, 4.0, 10.0, 22.0, 25.0],
     [29.0, 2.0, 7.0, 1.0, 3.0, 2.0, 6.0],
     [34.0, 2.0, 5.0, 1.0, 1.0, 2.0, 4.0],
     [39.0, 21.0, 37.0, 3.0, 4.0, 10.0, 11.0],
     [37.0, 8.0, 23.0, 2.0, 9.0, 2.0, 3.0],
     [40.0, 18.0, 27.0, 2.0, 3.0, 6.0, 7.0],
     [47.0, 21.0, 31.0, 2.0, 6.0, 16.0, 22.0],
     [50.0, 22.0, 34.0, 7.0, 15.0, 20.0, 25.0],
     [24.0, 7.0, 7.0, 5.0, 5.0, 5.0, 5.0]], dtype=float)
points = np.array(
    [5.0, 11.0, 23.0, 36.0, 81.0, 13.0, 8.0, 62.0, 7.0, 7.0, 55.0, 20.0, 44.0, 60.0, 71.0, 24.0],
    dtype=float)

model.fit(gamelog, points, epochs=500)
print('weights:', model.weights)
# Should print 24 = 2 x 9 + 6
print(model.predict([[47.0, 9.0, 28.0, 0.0, 7.0, 6.0, 10.0]]))