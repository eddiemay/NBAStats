package com.digitald4.nbastats.machinelearning;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;

public class ComputationPy {
  public static void main(String[] args) {
    SavedModelBundle model = SavedModelBundle.load("./target/adder", "serve");
    Tensor<Integer> tensor = model.session().runner().fetch("inp")
        .feed("x", Tensor.create(3, Integer.class))
        .run().get(0).expect(Integer.class);
    System.out.println(tensor.intValue());
  }
}
