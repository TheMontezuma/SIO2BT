package org.atari.montezuma.sio2bt;

import java.io.IOException;

public interface IDataFrameReader
{
	public DataBuffer readDataFrame(int count) throws IOException;
}
