package com.sovworks.eds.fs;

import java.io.IOException;
import java.util.Date;

public interface FSRecord
{
	Path getPath();
    String getName() throws IOException;
    void rename(String newName) throws IOException;
	Date getLastModified() throws IOException;
    void setLastModified(Date dt) throws IOException;
    
    void delete() throws IOException;
    void moveTo(Directory newParent) throws IOException;
}
