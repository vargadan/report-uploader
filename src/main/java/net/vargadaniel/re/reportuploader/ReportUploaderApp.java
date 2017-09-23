package net.vargadaniel.re.reportuploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportUploaderApp {
	
	public static void main(String... args) {
		String k8sNamespace = System.getenv("KUBERNETES_NAMESPACE");
		if (k8sNamespace != null) {
			String profile = k8sNamespace.substring(k8sNamespace.lastIndexOf("-") + 1);
			System.setProperty("spring.profiles.active", profile);
		}
		SpringApplication.run(ReportUploaderApp.class, args);
	}

}
