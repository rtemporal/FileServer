module temporal.file.client
{
	requires java.logging;
	requires java.rmi;
	requires transitive javafx.controls;
	requires transitive javafx.fxml;
	requires temporal.file.environment;
	requires transitive temporal.file.server;

	exports temporal.file.client;
	opens temporal.file.client to javafx.fxml;
}
