package net.indiespot.loadbalancer.util;

import java.io.File;
import java.io.IOException;

public abstract class LiveFile
{
	private final File file;
	private byte[] data;
	private final long interval;
	private long nextCheck;
	private long lastMod;

	public LiveFile(File file, long interval)
	{
		if(interval <= 0)
		{
			throw new IllegalArgumentException("interval: " + interval);
		}
		if(file == null || !file.exists() || !file.canRead() || file.isDirectory())
		{
			throw new IllegalStateException("can't read file: " + file);
		}
		this.file = file;
		this.interval = interval;
	}

	private long now()
	{
		return System.currentTimeMillis();
	}

	public byte[] data()
	{
		out: if(data == null || nextCheck <= now())
		{
			nextCheck = now() + interval;

			if(lastMod == file.lastModified())
				break out;
			lastMod = file.lastModified();

			System.out.println("LiveFile modification: " + file);

			try
			{
				data = Streams.readFile(file);
				onNewData(data);
			}
			catch (IOException exc)
			{
				if(data == null)
					throw new IllegalStateException(exc);
				// retain last data
				exc.printStackTrace();
			}
		}
		return data;
	}

	public abstract void onNewData(byte[] data);
}
