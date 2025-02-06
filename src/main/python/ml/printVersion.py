import tensorflow as tf

x = [[2.]]

print("TensorFlow version: {}".format(tf.__version__))
print(tf.config.experimental.list_physical_devices('GPU'))
tensor = tf.constant([])
print(tensor.device)
# print("Eager execution: {}".format(tf.executing_eagerly()))
print('hello, {}'.format(tf.matmul(x, x)))