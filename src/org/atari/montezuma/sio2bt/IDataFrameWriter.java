package org.atari.montezuma.sio2bt;

import java.io.IOException;

public interface IDataFrameWriter
{
	public void writeDataFrame(DataBuffer buffer) throws IOException;
	public void writeCommandAck() throws IOException;
	public void writeDataAck() throws IOException;
	public void writeCommandNack() throws IOException;
	public void writeDataNack() throws IOException;
	public void writeComplete() throws IOException;
	public void writeError() throws IOException;
}
