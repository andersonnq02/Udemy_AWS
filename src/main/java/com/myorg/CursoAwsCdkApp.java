package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class CursoAwsCdkApp {
	public static void main(final String[] args) {
		App app = new App();

//        new CursoAwsCdkStack(app, "CursoAwsCdkStack", StackProps.builder().build());

		VpcStack vpcStack = new VpcStack(app, "Vpc");

		ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
		clusterStack.addDependency(vpcStack);
		
		Service01Stack service01Stack = new Service01Stack(app, "Service01", clusterStack.getCluster());
		service01Stack.addDependency(clusterStack);

		app.synth();
	}
}
