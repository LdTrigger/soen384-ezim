/*
    EZ Intranet Messenger

    Copyright (C) 2007 - 2008  Chun-Kwong Wong
    chunkwong.wong@gmail.com
    http://ezim.sourceforge.net/

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ezim.core;

import java.lang.Runnable;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.ezim.core.Ezim;
import org.ezim.core.EzimConf;
import org.ezim.core.EzimContact;
import org.ezim.core.EzimContactList;
import org.ezim.core.EzimDtxTakerThread;
import org.ezim.core.EzimThreadPool;
import org.ezim.core.EzimLogger;
import org.ezim.ui.EzimMain;

public class EzimDtxTaker implements Runnable
{
	public EzimDtxTaker()
	{
	}

	public void run()
	{
		ServerSocket ssck = null;
		Socket sckIn = null;

		EzimConf ecnfTmp = EzimConf.getInstance();

		try
		{
			ssck = new ServerSocket
			(
				Integer.parseInt
				(
					ecnfTmp.settings.getProperty(EzimConf.ezimDtxPort)
				)
			);
		}
		catch(Exception e)
		{
			EzimMain.getInstance().errAlert(e.getMessage());
			EzimLogger.getInstance().severe(e.getMessage(), e);

			try
			{
				if (ssck != null && ! ssck.isClosed()) ssck.close();
			}
			catch(Exception exp)
			{
				EzimLogger.getInstance().severe(exp.getMessage(), exp);
			}

			// only one instance of the application is allowed at one time
			System.exit(1);
		}

		try
		{
			while(true)
			{
				sckIn = ssck.accept();
				sckIn.setSoTimeout(Ezim.dtxTimeout);

				EzimContact ecTmp = EzimContactList.getInstance().getContact
				(
					((InetSocketAddress) sckIn.getRemoteSocketAddress())
						.getAddress().getHostAddress()
				);

				// only take messages from known contacts
				if (ecTmp != null)
				{
					EzimDtxTakerThread emttTmp = new EzimDtxTakerThread
					(
						ecTmp
						, sckIn
					);

					EzimThreadPool etpTmp = EzimThreadPool.getInstance();

					etpTmp.execute(emttTmp);
				}
				else if (sckIn != null && ! sckIn.isClosed())
				{
					sckIn.close();
					sckIn = null;
				}
			}
		}
		catch(Exception e)
		{
			EzimMain.getInstance().errAlert(e.getMessage());
			EzimLogger.getInstance().severe(e.getMessage(), e);
		}
		finally
		{
			try
			{
				if (ssck != null && ! ssck.isClosed()) ssck.close();
			}
			catch(Exception e)
			{
				EzimLogger.getInstance().severe(e.getMessage(), e);
			}
		}
	}
}
