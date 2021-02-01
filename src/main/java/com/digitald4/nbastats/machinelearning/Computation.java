package com.digitald4.nbastats.machinelearning;

import org.tensorflow.DataType;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class Computation {
	public static void main(String[] args) {
		try (Graph graph = new Graph()) {
			// Construct the computation graph with a single operation, a constant
			// named "MyConst" with a value "value".
			// The Java API doesn't yet include convenience functions for adding operations.
			Operation a = graph.opBuilder("Const", "a")
					.setAttr("dtype", DataType.fromClass(Integer.class))
					.setAttr("value", Tensor.create(3, Integer.class))
					.build();
			Operation b = graph.opBuilder("Const", "b")
					.setAttr("dtype", DataType.fromClass(Integer.class))
					.setAttr("value", Tensor.create(2, Integer.class))
					.build();
			Operation x = graph.opBuilder("Placeholder", "x")
					.setAttr("dtype", DataType.fromClass(Integer.class))
					.build();
			Operation y = graph.opBuilder("Placeholder", "y")
					.setAttr("dtype", DataType.fromClass(Integer.class))
					.build();
			Operation ax = graph.opBuilder("Mul", "ax")
					.addInput(a.output(0))
					.addInput(x.output(0))
					.build();
			Operation by = graph.opBuilder("Mul", "by")
					.addInput(b.output(0))
					.addInput(y.output(0))
					.build();
			Operation z = graph.opBuilder("Add", "z")
					.addInput(ax.output(0))
					.addInput(by.output(0))
					.build();
			System.out.println(z.output(0));

			// Execute the "MyConst" operation in a Session.
			try (Session session = new Session(graph)) {
				Tensor<Integer> tensor = session.runner().fetch("z")
						.feed("x", Tensor.create(3, Integer.class))
						.feed("y", Tensor.create(6, Integer.class))
						.run().get(0).expect(Integer.class);
				System.out.println(tensor.intValue());
			}
		}
	}
}
