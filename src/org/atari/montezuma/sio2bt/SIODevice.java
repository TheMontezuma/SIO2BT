package org.atari.montezuma.sio2bt;

import java.io.IOException;

public abstract class SIODevice
{
	/**
	 * 
	 * @param command
	 * @param aux1
	 * @param aux2
	 * @param reader
	 * @param writer
	 * @return true if command changed the device (for example formating a floppy), so HMI update is required, false otherwise
	 * @throws IOException
	 */
	public abstract boolean processSIOCommand(int command, int aux1, int aux2, IDataFrameReader reader, IDataFrameWriter writer) throws IOException;
	
    public abstract String getDisplayName();
    public abstract String getDescription();
    public abstract void finalize();
}
