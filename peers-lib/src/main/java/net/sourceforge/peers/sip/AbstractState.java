/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008, 2009, 2010 Yohann Martineau 
*/

package net.sourceforge.peers.sip;

import net.sourceforge.peers.Logger;

public abstract class AbstractState {
    
    protected String id;
    protected Logger logger;
    
    protected AbstractState(String id, Logger logger) {
        this.id = id;
        this.logger = logger;
    }

    public void log(AbstractState state) {
        String buf = "SM " + id + " [" +
                JavaUtils.getShortClassName(this.getClass()) + " -> " +
                JavaUtils.getShortClassName(state.getClass()) + "] " +
                new Exception().getStackTrace()[1].getMethodName();
        logger.debug(buf);
    }
    
}
