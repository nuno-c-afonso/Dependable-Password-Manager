package pt.tecnico.sec.dpm.server;

import javax.xml.ws.Endpoint;

import pt.tecnico.sec.dpm.server.exceptions.NullArgException;

//import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

public class ServerApplication {

	public static void main(String[] args) {
		// Check arguments
		/*if (args.length < 3) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s uddiURL wsName wsURL%n", ServerApplication.class.getName());
			return;
		}*/
		
		// Check arguments
		if (args.length == 0) {
			System.err.println("Argument missing!");
			System.err.printf("Usage: java %s wsURL [dbIndex]%n", ServerApplication.class.getName());
			return;
		}
		
		//String uddiURL = args[0];
		//String name = args[1];
		//String url = args[2];

		String url = args[0];
		int dbIndex = -1;
		APIImpl service = null;
		
		if(args.length > 1)
			dbIndex = new Integer(args[1]);
		
		try {
			if(dbIndex == -1)
				service = new APIImpl(url, "ins3cur3".toCharArray(), "1nsecure".toCharArray());
			else
				service = new APIImpl(url, "ins3cur3".toCharArray(), "1nsecure".toCharArray(), dbIndex);
		} catch(NullArgException e) {
			e.printStackTrace();
			return;
		}
		
		Endpoint endpoint = null;
		//UDDINaming uddiNaming = null;
		try {
			endpoint = Endpoint.create(service);

			// publish endpoint
			System.out.printf("Starting %s%n", url);
			endpoint.publish(url);

			// publish to UDDI
			//System.out.printf("Publishing '%s' to UDDI at %s%n", name, uddiURL);
			//uddiNaming = new UDDINaming(uddiURL);
			//uddiNaming.rebind(name, url);

			// wait
			System.out.println("Awaiting connections");
			System.out.println("Press enter to shutdown");
			System.in.read();

		} catch (Exception e) {
			System.out.printf("Caught exception: %s%n", e);
			e.printStackTrace();

		} finally {
			try {
				if (endpoint != null) {
					// stop endpoint
					endpoint.stop();
					service.close();
					System.out.printf("Stopped %s%n", url);
				}
			} catch (Exception e) {
				System.out.printf("Caught exception when stopping: %s%n", e);
			}
			/*try {
				if (uddiNaming != null) {
					// delete from UDDI
					uddiNaming.unbind(name);
					System.out.printf("Deleted '%s' from UDDI%n", name);
				}
			} catch (Exception e) {
				System.out.printf("Caught exception when deleting: %s%n", e);
			}*/
		}

	}

}
