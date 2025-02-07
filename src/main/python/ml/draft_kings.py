# Finds how basketball scoring is done from inputs of:
# Minutes played, field goals made, field goal attempts, 3 point baskets made,
# 3 point attempts, free throws made and free throw attempts.
import keras
import numpy as np
import pandas as pd


# Make numpy values easier to read.
np.set_printoptions(precision=3, suppress=True)

gamelog = pd.read_csv("../../../../data/lebron_gamelog.csv")
gamelog.head()
print(gamelog)

gamelog_features = gamelog.copy()
points = gamelog_features.pop('DKP')
gamelog_features.pop('Player')
gamelog_features.pop('Date')
gamelog_features.pop('PTS')
gamelog_features.pop('FDP')

print(gamelog_features)
print(points)

model = keras.Sequential([keras.layers.Dense(units=1, input_shape=[18])])
model.compile(optimizer=keras.optimizers.Adam(1.0), loss='mean_squared_error')

model.fit(gamelog_features, points, epochs=500)
print('weights:', model.weights)
print('Desired output: 61.5')
print('Predict: ', model.predict(np.array([[46.8, 9.0, 28.0, 0.0, 7.0, 6.0, 10.0, 6.0, 10.0, 16.0, 9.0, 2.0, 0.0, 3.0, 0.0, -3.0, 1, 0]])))