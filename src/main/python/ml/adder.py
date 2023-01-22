import tensorflow as tf

class Adder(tf.Module):

    @tf.function(input_signature=[tf.TensorSpec(shape=None, dtype=tf.float32)])
    def add(self, x):
        return x + x + 1.

m = Adder()
tf.saved_model.save(
    m, './target/adder',
    signatures = m.add.get_concrete_function(
        tf.TensorSpec(shape=[None, 3], dtype=tf.float32, name="add")))

print("TensorFlow version:", tf.__version__)