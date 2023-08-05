# Finds how basketball scoring is done from inputs of:
# Minutes played, field goals made, field goal attempts, 3 point baskets made,
# 3 point attempts, free throws made and free throw attempts.
import tensorflow as tf
import pandas as pd
import numpy as np
from tensorflow import keras

# Make numpy values easier to read.
np.set_printoptions(precision=3, suppress=True)

gamelog = pd.read_csv("data/lebron_gamelog.csv")
gamelog.head()
print(gamelog)

gamelog_features = gamelog.copy()
points = gamelog_features.pop('FDP')
gamelog_features.pop('Player')
gamelog_features.pop('Date')
gamelog_features.pop('PTS')
gamelog_features.pop('DKP')

print(gamelog_features)
print(points)

model = tf.keras.Sequential([keras.layers.Dense(units=1, input_shape=[18])])
model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')

model.fit(gamelog_features, points, epochs=500)
print('weights:', model.weights)
print('Desired output: 59.7')
print('Predict: ', model.predict([[46.8, 9.0, 28.0, 0.0, 7.0, 6.0, 10.0, 6.0, 10.0, 16.0, 9.0, 2.0, 0.0, 3.0, 0.0, -3.0, 1, 0]]))