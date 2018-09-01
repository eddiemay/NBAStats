from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import os
import shutil
import sys

import tensorflow as tf  # pylint: disable=g-bad-import-order

from official.utils.arg_parsers import parsers
from official.utils.logs import hooks_helper
from official.utils.misc import model_helpers

_CSV_COLUMNS = [
    'age', 'pts', 'mins', 'fga', 'fgm',
    '3pa', '3pm', 'fta', 'ftm', 'rebs',
    'ast', 'stl', 'to', 'blk', 'team'
]

_CSV_COLUMN_DEFAULTS = [[24], [0], [0], [0], [0], [0], [0], [0], [0], [0],
                        [0], [0], [0], [0], ['']]

_NUM_EXAMPLES = {
    'train': 200,
    'validation': 100,
}


LOSS_PREFIX = {'wide': 'linear/', 'deep': 'dnn/'}


def build_model_columns():
  """Builds a set of wide and deep feature columns."""
  # Continuous columns
  age = tf.feature_column.numeric_column('age')
  pts = tf.feature_column.numeric_column('pts')
  mins = tf.feature_column.numeric_column('mins')
  fga = tf.feature_column.numeric_column('fga')
  fgm = tf.feature_column.numeric_column('fgm')
  tpa = tf.feature_column.numeric_column('3pa')
  tpm = tf.feature_column.numeric_column('3pm')
  fta = tf.feature_column.numeric_column('fta')
  ftm = tf.feature_column.numeric_column('ftm')
  rebs = tf.feature_column.numeric_column('rebs')
  ast = tf.feature_column.numeric_column('ast')
  stl = tf.feature_column.numeric_column('stl')
  to = tf.feature_column.numeric_column('to')
  blk = tf.feature_column.numeric_column('blk')
  team_num = tf.feature_column.numeric_column('team_num')

  team = tf.feature_column.categorical_column_with_vocabulary_list(
      'team', [
          'Lakers', 'Clippers', 'Warriors', 'Kings', 'Suns',
          'Rockets', 'Mavericks', 'Spurs', 'Pelicans', 'Grizzles'
          'Blazers', 'Thunder', 'Jazz', 'Nuggets', 'Timberwolves',
          'Heat', 'Magic', 'Hawks', 'Wizards', 'Hornets',
          'Celtics', 'Knicks', 'Nets', '76ers', 'Raptors'
          'Bulls', 'Cavs', 'Pistons', 'Bucks', 'Pacers'])

  # Transformations.
  age_buckets = tf.feature_column.bucketized_column(
      age, boundaries=[18, 25, 30, 35, 40, 45, 50, 55, 60, 65])

  return [ags, pts, mins, fga, fgm, tpa, tpm, fta, ftm, rebs, ast, stl, to, blk, team_num]


def build_estimator(model_dir, model_type):
  """Build an estimator appropriate for the given model type."""
  wide_columns, deep_columns = build_model_columns()
  hidden_units = [100, 75, 50, 25]

  # Create a tf.estimator.RunConfig to ensure the model is run on CPU, which
  # trains faster than GPU for this model.
  run_config = tf.estimator.RunConfig().replace(
      session_config=tf.ConfigProto(device_count={'GPU': 0}))

  if model_type == 'wide':
    return tf.estimator.LinearClassifier(
        model_dir=model_dir,
        feature_columns=wide_columns,
        config=run_config)
  elif model_type == 'deep':
    return tf.estimator.DNNClassifier(
        model_dir=model_dir,
        feature_columns=deep_columns,
        hidden_units=hidden_units,
        config=run_config)
  else:
    return tf.estimator.DNNLinearCombinedClassifier(
        model_dir=model_dir,
        linear_feature_columns=wide_columns,
        dnn_feature_columns=deep_columns,
        dnn_hidden_units=hidden_units,
        config=run_config)


def input_fn(data_file, num_epochs, shuffle, batch_size):
  """Generate an input function for the Estimator."""
  assert tf.gfile.Exists(data_file), (
      '%s not found. Please make sure you have run data_download.py and '
      'set the --data_dir argument to the correct path.' % data_file)

  def parse_csv(value):
    print('Parsing', data_file)
    columns = tf.decode_csv(value, record_defaults=_CSV_COLUMN_DEFAULTS)
    features = dict(zip(_CSV_COLUMNS, columns))
    labels = features.pop('income_bracket')
    return features, tf.equal(labels, '>50K')

  # Extract lines from input files using the Dataset API.
  dataset = tf.data.TextLineDataset(data_file)

  if shuffle:
    dataset = dataset.shuffle(buffer_size=_NUM_EXAMPLES['train'])

  dataset = dataset.map(parse_csv, num_parallel_calls=5)

  # We call repeat after shuffling, rather than before, to prevent separate
  # epochs from blending together.
  dataset = dataset.repeat(num_epochs)
  dataset = dataset.batch(batch_size)
  return dataset


def main(argv):
  parser = WideDeepArgParser()
  flags = parser.parse_args(args=argv[1:])

  # Clean up the model directory if present
  shutil.rmtree(flags.model_dir, ignore_errors=True)
  model = build_estimator(flags.model_dir, flags.model_type)

  train_file = os.path.join(flags.data_dir, 'adult.data')
  test_file = os.path.join(flags.data_dir, 'adult.test')

  # Train and evaluate the model every `flags.epochs_between_evals` epochs.
  def train_input_fn():
    return input_fn(
        train_file, flags.epochs_between_evals, True, flags.batch_size)

  def eval_input_fn():
    return input_fn(test_file, 1, False, flags.batch_size)

  loss_prefix = LOSS_PREFIX.get(flags.model_type, '')
  train_hooks = hooks_helper.get_train_hooks(
      flags.hooks, batch_size=flags.batch_size,
      tensors_to_log={'average_loss': loss_prefix + 'head/truediv',
                      'loss': loss_prefix + 'head/weighted_loss/Sum'})

  # Train and evaluate the model every `flags.epochs_between_evals` epochs.
  for n in range(flags.train_epochs // flags.epochs_between_evals):
    model.train(input_fn=train_input_fn, hooks=train_hooks)
    results = model.evaluate(input_fn=eval_input_fn)

    # Display evaluation metrics
    print('Results at epoch', (n + 1) * flags.epochs_between_evals)
    print('-' * 60)

    for key in sorted(results):
      print('%s: %s' % (key, results[key]))

    if model_helpers.past_stop_threshold(
        flags.stop_threshold, results['accuracy']):
      break


class WideDeepArgParser(argparse.ArgumentParser):
  """Argument parser for running the wide deep model."""

  def __init__(self):
    super(WideDeepArgParser, self).__init__(parents=[parsers.BaseParser()])
    self.add_argument(
        '--model_type', '-mt', type=str, default='wide_deep',
        choices=['wide', 'deep', 'wide_deep'],
        help='[default %(default)s] Valid model types: wide, deep, wide_deep.',
        metavar='<MT>')
    self.set_defaults(
        data_dir='/tmp/census_data',
        model_dir='/tmp/census_model',
        train_epochs=40,
        epochs_between_evals=2,
        batch_size=40)


if __name__ == '__main__':
  tf.logging.set_verbosity(tf.logging.INFO)
  main(argv=sys.argv)