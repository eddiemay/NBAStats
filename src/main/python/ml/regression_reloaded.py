import numpy as np
import pandas as pd
import tensorflow as tf

# Make numpy printouts easier to read.
np.set_printoptions(precision=3, suppress=True)

# Load the dataset.
url = 'http://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data'
column_names = ['MPG', 'Cylinders', 'Displacement', 'Horsepower', 'Weight',
                'Acceleration', 'Model Year', 'Origin']

raw_dataset = pd.read_csv(url, names=column_names,
                          na_values='?', comment='\t',
                          sep=' ', skipinitialspace=True)

# Make a copy of the dataset.
dataset = raw_dataset.copy()
print(dataset.tail())

# Drop bad data that sums to zero.
print(dataset.isna().sum())
dataset = dataset.dropna()

# Break out the Origin column.
dataset['Origin'] = dataset['Origin'].map({1: 'USA', 2: 'Europe', 3: 'Japan'})
dataset = pd.get_dummies(dataset, columns=['Origin'], prefix='', prefix_sep='')
print(dataset.tail())

# Split into train and test datasets.
train_dataset = dataset.sample(frac=0.8, random_state=0)
test_dataset = dataset.drop(train_dataset.index)

test_features = test_dataset.copy()

test_labels = test_features.pop('MPG')
print('Test labels:')
print(test_labels)

reloaded = tf.keras.models.load_model('dnn_model')
test_results = {}

test_predictions = reloaded.predict(test_features).flatten()
print('Test predictions:')
print(test_predictions)

test_results['reloaded'] = reloaded.evaluate(test_features, test_labels, verbose=0)

print(pd.DataFrame(test_results, index=['Mean absolute error [MPG]']).T)