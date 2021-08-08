from __future__ import absolute_import, division, print_function

import os
import tensorflow as tf

x = [[2.]];

print("TensorFlow version: {}".format(tf.__version__))
# print("Eager execution: {}".format(tf.executing_eagerly()))
print('hello, {}'.format(tf.matmul(x, x)))