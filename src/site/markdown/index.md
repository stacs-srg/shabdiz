#Shabdiz
Shabdiz is a simple tool that maintains a given application running on a specified set of machines. It probes each machine periodically to establish whether the application appears to be running. If not, it pushes the application software to the machine via SSH and instantiates the application.

The Shabdiz tool is written in Java. It is configured for a specific application by writing a simple controller in Java. The application itself need not be implemented in Java.

The requirements for a monitored machine are SSH access, a bash shell, and the wget tool. There is no need to install any Shabdiz components to the monitored machines before use.

Other Shabdiz features:

- control via an API or a web interface
- specification of machines to be monitored using address ranges
- manual or automatic application deployment
- killing of application instances
- hooks for application-specific control operations


