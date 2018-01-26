package org.springframework.cloud.devtools.sshproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;

// Need to setup two things.
// 1) The cf port forwarding from localhost 2225 into the container on localhost:9099 (where the Apache mina is running)
//    Can be done via:
//    	cf ssh zzz-ac -v -L 2225:localhost:9099
//    or with ssh using a one time code:
//      export GUID=`cf app zzz-ac --guid`
//      cf ssh-code | pbcopy
//      ssh -v cf:$GUID/0@ssh.run.pivotal.io -A -p 2222 -L 2225:localhost:9099
//
//    From the shell that gets created can look at the available interfaces
// 		/sbin/ifconfig -a | head
//
// 2) Reverse forwarding from 8080 on the container to localhost where the boot app is running 
//  ssh -4 -N -vvv localhost -p 2225 -R 10.252.85.233:8080:127.0.0.1:65228

// The use of ProcessBuilder in here should probably be replaced with cf java client and 
// a real ssh java client library. The only trickyness may be around the need to run the ifconfig
// to gain the right interface address

// Or in fact maybe this should all be pushed into a cf cli plugin alongside the one that
// deploys the proxy that this talks to.

/**
 * @author Andy Clement
 */
public class TunnelConnectionManager implements DisposableBean {

	private String appname;
	private EmbeddedServletContainer embeddedServletContainer;
	
	private boolean showProcessOutput = true;
		
	public TunnelConnectionManager(String appname, EmbeddedServletContainer embeddedServletContainer) {
		this.appname = appname;//+"-proxy";
		this.embeddedServletContainer = embeddedServletContainer;
	}
	
	private class JVMShutdownHook extends Thread {
	    public void run() {
	    		TunnelConnectionManager.this.stop();
	    }
	  }
	
	LocalRemoteConnection localRemoteConnection = new LocalRemoteConnection();
	
	ReverseTunnelConnection reverseTunnelConnection = new ReverseTunnelConnection();
	
	public void start() {
		if (appname == null) {
			System.out.println("TUNNELING NOT ACTIVATED: set " + 
					"spring.cloud.devtools.tunnel.deployed-app-name=<app>");
			return;
		}
		// Crude way to just do this later, once the port is available...
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Sleeping a moment...");
				long stime = System.currentTimeMillis();
				while ((System.currentTimeMillis()-stime)<20000) { // wait up to 20 seconds
					int num = embeddedServletContainer.getPort();
					if (num>0) {
						break;
					}
					try {
						Thread.sleep(500);
					} catch (Exception e) {
					}
				}
				System.out.println("Starting local port forward to remote embedded sshd (waited "+(System.currentTimeMillis()-stime)+"ms)");
				localRemoteConnection.setAppName(appname);
				localRemoteConnection.start(showProcessOutput);
				System.out.println("Creating reverse tunnel from CF app ("+localRemoteConnection.getInetAddr()+") to local port "+embeddedServletContainer.getPort());
				reverseTunnelConnection.setHostAddress(localRemoteConnection.getInetAddr());
				reverseTunnelConnection.setLocalPort(embeddedServletContainer.getPort());
				reverseTunnelConnection.start(showProcessOutput);
				JVMShutdownHook jvmShutdownHook = new JVMShutdownHook();
			    Runtime.getRuntime().addShutdownHook(jvmShutdownHook);
			}
			
		});
		t.start();
	}

	public void stop() {
		System.out.println("Shutting down all magic");
		localRemoteConnection.stop();
		reverseTunnelConnection.stop();
	}
	
	static abstract class CommandRunner {

		protected Process process;

		InputStream outputStreamFromProcess;
		
		OutputStream inputStreamToProcess;
		
		boolean showProcessOutput;
		
		static class OutputResponse {
			final boolean success;
			final String lastK;
			public OutputResponse(boolean b, String lastK2) {
				this.success =b;
				this.lastK = lastK2;
			}
		}
		
		protected OutputResponse readOutputStream(InputStream stream, String stopOnString, int timeoutInMilliseconds) {
			byte[] bs = new byte[10000];
			int i = 0;
			long lastDataTime = System.currentTimeMillis();
			String lastK = new String();
			try {
				while (true) {
					if (stream.available() != 0) {
						lastDataTime = System.currentTimeMillis();
						i = stream.read(bs);
						String s = new String(bs,0,i);
						if (showProcessOutput) {
							System.out.print(s);
						}
						lastK = lastK + s;
						if (lastK.length()>1000) {
							lastK = lastK.substring(lastK.length()-1000);
						}
						if (lastK.contains(stopOnString)) {
//							System.out.println("\nFound expected string: "+stopOnString);
							return new OutputResponse(true,lastK);
						}
					}
					if ((System.currentTimeMillis()-lastDataTime) > timeoutInMilliseconds) {
						System.out.println("Timeout, no output for "+timeoutInMilliseconds);
						return new OutputResponse(false,lastK);
					}
					try { Thread.sleep(100); } catch(Exception e) {}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new OutputResponse(false,lastK);
		}

		public void stop() {
			if (process != null) {
				process.destroy();
			}
		}
	}
	
//	ssh -o StrictHostKeyChecking=no -4 -N -vvv localhost -p 2225 -R 10.252.85.233:8080:127.0.0.1:65228
	static class ReverseTunnelConnection extends CommandRunner {
		
		private int localPort;

		private String hostAddress;
		
		public ReverseTunnelConnection() {
		}
		
		public void setLocalPort(int i) {
			this.localPort = i;
		}

		public void setHostAddress(String string) {
			this.hostAddress = string;
		}

		// Note the use of 'sshpass' here. That is due to the lack of proper security between the client
		// and server connections, we should use something better.
		public void start(boolean showProcessOutput) {
			this.showProcessOutput = showProcessOutput;
			// Example: ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -4 -N -vvv localhost -p 2225 -R 10.252.85.233:8080:127.0.0.1:65228
			ProcessBuilder pb = new ProcessBuilder().
					command("sshpass","-p","foobar","ssh","-o","StrictHostKeyChecking=no","-o","UserKnownHostsFile=/dev/null","-4","-N","-v","localhost","-p","2225",
							"-R",hostAddress+":8080:127.0.0.1:"+localPort);
			try {
				process = pb.start();
				inputStreamToProcess = process.getOutputStream();
//				inputStreamToProcess.close();
				outputStreamFromProcess = process.getInputStream();
				readOutputStream(process.getErrorStream(),"All remote forwarding requests processed",2000);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void stop() {
			try {
				if (inputStreamToProcess != null) {
					inputStreamToProcess.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			process.destroyForcibly();
		}
	}
	
	static class LocalRemoteConnection extends CommandRunner {
		
		private String appName;

		private Process process;
		
		InputStream outputStreamFromProcess;
		
		OutputStream inputStreamToProcess;

		private String inetAddr;
		
		public LocalRemoteConnection() {
		}
		
		public void start(boolean showProcessOutput) {
			this.showProcessOutput = showProcessOutput;
			// Example: cf ssh zzz-ac -v --force-pseudo-tty -L 2225:localhost:9099
			System.out.println("AppName is "+this.appName);
			ProcessBuilder pb = new ProcessBuilder().
					command("cf","ssh",this.appName,"-v", "--force-pseudo-tty", "-L","2225:localhost:9099");
			try {
				process = pb.start();
				inputStreamToProcess = process.getOutputStream();
				outputStreamFromProcess = process.getInputStream();
				OutputResponse response = readOutputStream(outputStreamFromProcess,":~$",5000);
				if (response.success) {
//					System.out.println("Attempting shell interaction...");
				
					// Attempt to grab inet address for eth0:
					// /sbin/ifconfig eth0 | grep "inet addr" | sed 's/^[^:]*:\([^ ]*\).*$/\1/'
					inputStreamToProcess.write("/sbin/ifconfig eth0 | grep \"inet addr\" | sed 's/^[^:]*:\\([^ ]*\\).*$/>>\\1<</'\n".getBytes());
					inputStreamToProcess.flush();
					OutputResponse or = readOutputStream(outputStreamFromProcess,":~$",5000);
					this.inetAddr = or.lastK.substring(or.lastK.lastIndexOf(">>")+2,or.lastK.lastIndexOf("<<"));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void stop() {
			try {
				if (inputStreamToProcess != null) {
					inputStreamToProcess.write("exit\n".getBytes());
					inputStreamToProcess.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			super.stop();
		}
		
		public void setAppName(String appName) {
			this.appName = appName;
		}
		
		public String getInetAddr() {
			return this.inetAddr;
		}
	}

	public void setShowProcessOutput(boolean s) {
		this.showProcessOutput = s;
	}

	@Override
	public void destroy() throws Exception {
		stop();
	}
	
}
