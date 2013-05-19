package geidsvig

class RestServerBoot extends akka.kernel.Bootable {

  def startup = {
    val server = new ApplicationRestServer
    server.run()
  }

  def shutdown = {
    ApplicationContext.system.shutdown()
  }
}
