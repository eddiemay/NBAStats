import tensorflow as tf
graph = tf.Graph()
builder = tf.saved_model.builder.SavedModelBuilder('./target/compute')
with graph.as_default():
    a = tf.constant(2, name='a')
    b = tf.constant(3, name='b')
    x = tf.placeholder(tf.int32, name='x')
    y = tf.placeholder(tf.int32, name='y')
    z = tf.math.add(a*x, b*y, name='z')
    sess = tf.Session()
    sess.run(z, feed_dict = {x: 2, y: 3})
    builder.add_meta_graph_and_variables(sess, [tf.saved_model.tag_constants.SERVING])
    builder.save()
