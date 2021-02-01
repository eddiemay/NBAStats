package com.digitald4.nbastats.machinelearning;

import java.nio.charset.StandardCharsets;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

public class HelloTF {
	public static void main(String[] args) throws Exception {
		try (Graph g = new Graph()) {
			final String value = "Hello from TensorFlow: " + TensorFlow.version();

			// Construct the computation graph with a single operation, a constant
			// named "MyConst" with a value "value".
			try (Tensor tensor = Tensor.create(value.getBytes(StandardCharsets.UTF_8))) {
				// The Java API doesn't yet include convenience functions for adding operations.
				g.opBuilder("Const", "MyConst")
						.setAttr("dtype", tensor.dataType())
						.setAttr("value", tensor).build();
			}

			// Execute the "MyConst" operation in a Session.
			try (Session session = new Session(g);
					 Tensor output = session.runner().fetch("MyConst").run().get(0)) {
				System.out.println(new String(output.bytesValue(), StandardCharsets.UTF_8));
			}
		}
	}
}
