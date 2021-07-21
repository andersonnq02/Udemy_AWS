package com.myorg;


import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
//Para criar o load balance precisa definir qual cluster ele estará
import software.amazon.awscdk.services.logs.LogGroup;

public class Service01Stack extends Stack {
	public Service01Stack(final Construct scope, final String id, Cluster cluster) {
		this(scope, id, null, cluster);
	}

	public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
		super(scope, id, props);

		// Ele não vai utilizar de isntância Ec2, vai especificar quantidade de memória
		// CPU e o app vai rodar em cima
		// desses recursos alocados
		ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
				.create(this, "ALB01")
				.serviceName("service01")
				.cluster(cluster)
				.cpu(512)//Medida relativa. É uma quantidade boa pra rodar springboot
				.memoryLimitMiB(1024)//memória na aplicação
				.desiredCount(2) //Quantidade de instancias desejadas de começo.
				.listenerPort(8080)//porta
				.taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                        .containerName("aws_project01")
                        .image(ContainerImage.fromRegistry("andersonnq02/curso_aws_project01:1.0.0"))
                        .containerPort(8080)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "Service01LogGroup")
                                        .logGroupName("Service01")
                                        .removalPolicy(RemovalPolicy.DESTROY)//Posso optar por não a pagar caso não precise mauis do serviços. Os logs no cloud watch
                                        .build())
                                .streamPrefix("Service01")
                                .build()))
                        .build())
				.publicLoadBalancer(true) //Tonar o acesso publico pois será colocado um DNS para o acesso ao serviço.
				.build();
		
		
		 //target group, é um recurso criado junto ao applicationLoadBalance, é resonsável por fazer verificação na aplicação para ver a saúde da instância. Caso contrário o serviço da instância destroi 
		 //biblioteca como dependência chamada actuator no gradle no microserviço.
		service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build());
		
		//Criar um autoscaling. Caso precise de recurso, cria mais uma instância. Caso não precise mais eu posso voltar.
		//Define a capacidade mínima e máxima
		ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

		//Vou dizer quais os parâmetros o AutoScaling vai atuar
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)//Se o consumo médio do meu CPU ultrapassar 50%
                .scaleInCooldown(Duration.seconds(60)) // No caso o consumo de 50% CPU ultrapassar em 60 segundos, cria uma nova instância no limite de 4 instâncias
                .scaleOutCooldown(Duration.seconds(60))// Período de análise que ele faz para poder destruir aquelas isntâncias criadas, caso o processamento não seja mais necessário.
                .build());

	}
}
