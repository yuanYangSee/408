package com.example.test4_0;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

public class DeviceDatas
{
	private static final String TAG = "DeviceDatas";
	
	private UsbDeviceConnection mConnection = null;
	private UsbManager mUsbManager = null;
	private UsbEndpoint mEndpointIn = null;
	private UsbEndpoint mEndpointOut = null;

	// 关闭连接，释放资源
	void CloseConnection(UsbDevice device)
	{
		Log.d(TAG, "CloseConnection");
		if ((this.mConnection != null) && (device != null))
		{
			this.mConnection.releaseInterface(device.getInterface(0));
			this.mConnection.close();
			this.mConnection = null;
			Log.d(TAG, "关闭连接，释放资源");
		}

	}
	
	//检查连接（也是保证已获取连接）
	boolean CheckConnection(UsbDevice device)
	{
		Log.d(TAG, "CheckConnection");
		boolean ret = false;
		try
		{
			if ((this.mConnection == null) && (device != null))
			{
				this.mConnection = GetConnection(device);
				if (this.mConnection == null)
				{
					Log.d(TAG, "mConnection==null");
					return false;
				}
				Log.d(TAG, "mConnection不为null");
				this.mConnection.claimInterface(device.getInterface(0), false);
				ret = true;
				return ret;
			}
			
		} catch (SecurityException e)
		{
			Log.e(TAG, "java.lang.SecurityException e: CheckConnection!!!");
		}
		ret=true;
		Log.d(TAG, "mConnection不为null");
		return ret;
	}
	
	private UsbDeviceConnection GetConnection(UsbDevice device)
	{
		UsbDeviceConnection connection = null;
		if (device == null)
		{
			Log.d(TAG, "Please insert USB flash disk!");
			return null;
		}
		if ((device != null) && (this.mUsbManager != null) && (this.mUsbManager.hasPermission(device)))
		{
			UsbInterface intf = device.getInterface(0);
			connection = this.mUsbManager.openDevice(device);
			if ((connection == null) || (!connection.claimInterface(intf, true)))
			{
				Log.e(TAG, "connection == null,open device failed!!");
				connection = null;
			}
		} else
		{
			Log.e("TAG", "usb has not Permission !");
		}
		return connection;
	}

	// 获取设备。
	UsbDevice GetDevice(Context mContext, int VendorId, int ProductId)
	{
		UsbDevice mdevice = null;
		Log.d(TAG, "getSystemService(Context.USB_SERVICE)");
		this.mUsbManager = ((UsbManager)mContext.getSystemService("usb"));
		if (this.mUsbManager == null)
		{
			Log.d(TAG, "mUsbManager == null return!!!");
			return null;
		}
		Log.d(TAG, "mUsbManager=" + this.mUsbManager);
		HashMap<String, UsbDevice> deviceList = this.mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while (deviceIterator.hasNext())
		{
			UsbDevice device = null;
			device = deviceIterator.next();
			String devInfo = device.getDeviceName() + "(" + device.getVendorId() + ":" + device.getProductId() + ")";
			Log.e(TAG, devInfo);
			if (VendorId == device.getVendorId() && ProductId == device.getProductId())
			{
				mdevice = device;
				Log.d(TAG, "mdevice found!");
				break;
			}
		}
		if(mdevice==null)
		{
			Log.d(TAG, "mdevice==null");
		}
		return mdevice;
	}

	// 建立连接
	public boolean makeConnection(Context mContext,UsbDevice device)
	{
		boolean isConnected = false;
		if (device == null)
		{
			Log.d(TAG, "Please insert USB flash disk!");
			Toast.makeText(mContext, "Please insert USB flash disk!", Toast.LENGTH_SHORT).show();
			return false;
		}
		// U盘接口个数为1
		if (device.getInterfaceCount() != 1)
		{
			Log.d(TAG, "Not a USB flash disk!");
			Toast.makeText(mContext, "Not a USB flash disk!", Toast.LENGTH_SHORT).show();
			return false;
		}

		UsbInterface intf = device.getInterface(0);

		// U盘接口0可获取的端点数为2
		if (intf.getEndpointCount() != 2)
		{
			Log.d(TAG, "Not a USB flash disk!");
			Toast.makeText(mContext, "Not a USB flash disk!", Toast.LENGTH_SHORT).show();
			return false;
		} else
		{
			mEndpointIn = intf.getEndpoint(0); // Bulk-In端点
			mEndpointOut = intf.getEndpoint(1); // Bulk_Out端点
			Log.d(TAG, "设备非空，获取到In以及OUT端点。");
		}

		if (device != null)
		{
			UsbDeviceConnection connection = mUsbManager.openDevice(device);
			if (connection != null && connection.claimInterface(intf, true))
			{
				Log.d(TAG, "连接非空，Make connection succeeded!");
				Toast.makeText(mContext, "Make connection succeeded!", Toast.LENGTH_SHORT).show();
				mConnection = connection;
				isConnected = true;
			} else
			{
				Log.d(TAG, "Make connection failed!");
				Toast.makeText(mContext, "Make connection failed!", Toast.LENGTH_SHORT).show();
				mConnection = null;
				return false;
			}
		}
		return isConnected;
	}
	
	//重置
	public void reset(Context mContext)
	{
		synchronized (this)
		{
			if (mConnection != null)
			{
				// 复位命令的设置有USB Mass Storage的定义文档给出
				int result = mConnection.controlTransfer(0x21, 0xFF, 0x00, 0x00, null, 0, 1000);
				if (result < 0)
				{ // result<0说明发送失败
					Log.d(TAG, "Send reset command failed!");
					DeviceIO.showToast(mContext, "复位失败", Toast.LENGTH_SHORT);
				} else
				{
					Log.d(TAG, "Send reset command succeeded!");
					DeviceIO.showToast(mContext, "复位成功", Toast.LENGTH_SHORT);
				}
			}
			else 
			{
				Log.d(TAG, "mConnection");
			}
			
		}
	}
	
	// 获取逻辑单元数
	public byte getMaxLnu()
	{
		byte i=0;
		synchronized (this)
		{
			if (mConnection != null)
			{
				// 接收的数据只有1个字节
				byte[] message = new byte[1];
				// 获取最大LUN命令的设置由USB Mass Storage的定义文档给出
				int result = mConnection.controlTransfer(0xA1, 0xFE, 0x00, 0x00, message, 1, 1000);
				if (result < 0)
				{
					Log.d(TAG, "Get max lnu failed!");
				} else
				{
					Log.d(TAG, "Get max lnu succeeded!");
					i=message[0] ;
				}
			}
		}
		return i;
	}
	
	//握手口令 0x02,0x01,0xef,0xef
	int UDiskVerfiy()
	{
		Log.d(TAG, "UDiskVerfiy()"); 
		int ret = -1;
		int i = 0;
		byte[] di_CSW = new byte[13];
		byte[] buffer=new byte[4];

		byte[] do_CBW = new byte[]
				{	    (byte) 0x55, (byte) 0x53, (byte) 0x42, (byte) 0x43, // 固定值 0~3
						(byte) 0x28, (byte) 0xe8, (byte) 0x3e, (byte) 0xfe, // 4~7自定义,与返回的CSW中的值是一样的
						(byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 8~11传输数据长度为4字节
						(byte) 0x80, 										// (12th 0x80-in)
						(byte) 0x00, 										// 13th LNU为0,则设为0
						(byte) 0x04, 										// 14th	命令长度 command length
						(byte) 2, (byte) 1, (byte) -17, (byte)-17, // READ	FORMAT CAPACITIES,后面的0x00皆被忽略
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,	//				
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		ret = mConnection.bulkTransfer(mEndpointOut, do_CBW, do_CBW.length, 1000);
		if (ret != 31)
	    {
	      Log.e(TAG, "1...UDiskVerfiy DO_CBW fail!\n");
	      return -301;
	    }
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, buffer, 4, 1000);
		}
		Log.e(TAG, "mEndpointIn ret=" + ret);
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, 1000);
		} 

		if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
		{
			Log.e(TAG, "2...UDiskVerfiy DI_CSW fail!\n");
			return -302;
		}
		for (i = 4; i < 8; i++)
		{
			if (di_CSW[i] == do_CBW[i])
				continue;
			Log.e(TAG, "4...UDiskVerfiy DI_CSW fail!\n");
			return -303;
		}
		Log.e(TAG, "buffer=" + Arrays.toString(buffer));
		return 0;
	}

	private int UDiskDownData(Byte[] cmd , int nTimeOut)
	{
		int ret = -1;
		int i = 0;
		byte[] di_CSW = new byte[13];
		
		byte[] do_CBW = new byte[]
				{	    (byte) 0x55, (byte) 0x53, (byte) 0x42, (byte) 0x43, // 固定值 0~3
						(byte) 0x28, (byte) 0xe8, (byte) 0x3e, (byte) 0xfe, // 4~7自定义,与返回的CSW中的值是一样的
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 8~11传输数据长度为512字节
						(byte) 0x00, 										// (12th 0x00-out)
						(byte) 0x00, 										// 13th LNU为0,则设为0
						(byte) 0x06, 										// 14th	命令长度 command length
						(byte) cmd[0], (byte) cmd[1], (byte) cmd[2], (byte)cmd[3], // READ	FORMAT CAPACITIES,后面的0x00皆被忽略
						(byte) cmd[4], (byte) cmd[5], (byte) 0x00, (byte) 0x00,	//				
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		if ((this.mEndpointOut != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointOut, do_CBW, 31, nTimeOut);
		}

		if (ret != 31)
		{
			Log.e(TAG, "1...UDiskDownData DO_CBW fail!\n");
			return -301;
		}

		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, nTimeOut);
		} 

		if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
		{
			Log.e(TAG, "2...UDiskDownData DI_CSW fail!\n");
			return -303;
		}
		for (i = 4; i < 8; i++)
		{
			if (di_CSW[i] == do_CBW[i])
				continue;
			Log.e("AS60xDatas", "4...UDiskDownData DI_CSW fail!\n");
			return -303;
		}
		
		Log.d(TAG, "2 UDiskDownData="+Arrays.toString(di_CSW));
		return 0;
	}
	
	

	private int UDiskGetData(byte[] upImgCmd,  ByteBuffer img, int img_Len, int nTimeOut )
	{
		Log.d(TAG, "UDiskGetData()"); 
		int ret = -1;
		int i = 0;
		//byte[] recvbuffer = new byte[65536];//64K
		byte[] recvbuffer = new byte[36];
		byte[] di_CSW = new byte[13];
		
		byte[] do_CBW = new byte[]
				{	    (byte) 0x55, (byte) 0x53, (byte) 0x42, (byte) 0x43, // 固定值 0~3
						(byte) 0x28, (byte) 0xe8, (byte) 0x3e, (byte) 0xfe, // 4~7自定义,与返回的CSW中的值是一样的
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // 8~11传输数据长度为512字节
						(byte) 0x80, 										// (12th 0x80-in)
						(byte) 0x00, 										// 13th LNU为0,则设为0
						(byte) 0x06, 										// 14th	命令长度 command length
						(byte) upImgCmd[0], (byte) upImgCmd[1], (byte) upImgCmd[2], (byte)upImgCmd[3], // READ	FORMAT CAPACITIES,后面的0x00皆被忽略
						(byte) upImgCmd[4], (byte) upImgCmd[5], (byte) 0x00, (byte) 0x00,	//				
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		do_CBW[8] = (byte) (img_Len & 0xFF);
		do_CBW[9] = (byte) (img_Len >> 8 & 0xFF);
		do_CBW[10] = (byte) (img_Len >> 16 & 0xFF);
		do_CBW[11] = (byte) (img_Len >> 24 & 0xFF);
		ret = mConnection.bulkTransfer(mEndpointOut, do_CBW, do_CBW.length, nTimeOut);
		if (ret != 31)
	    {
	      Log.e(TAG, "1...UDiskGetData DO_CBW fail!\n");
	      return -311;
	    }
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, recvbuffer, 36, nTimeOut);
		//	ret = this.mConnection.bulkTransfer(this.mEndpointIn, recvbuffer, recvbuffer.length, nTimeOut);
		}
		Log.e(TAG, "mEndpointIn ret=" + ret);
		
		img.put(recvbuffer, 0, recvbuffer.length);
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, nTimeOut);
		} 

		if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
		{
			Log.e(TAG, "2...UDiskDownData DI_CSW fail!\n");
			return -303;
		}
		for (i = 4; i < 8; i++)
		{
			if (di_CSW[i] == do_CBW[i])
				continue;
			Log.e(TAG, "4...UDiskDownData DI_CSW fail!\n");
			return -303;
		}
		return 0;
	}
	
	//上传图像
	int UdiskUpImage(ByteBuffer Buff, int Buff_length)
	  {
		Log.d(TAG, "UdiskUpImage()."); 
	 //   byte[] upImgCmd = { 0x01, 0x01, 0x00, 0x00, 0x24, 0x00}; 
	    byte[] upImgCmd = { 0x12, 0x00, 0x00, 0x00, 0x24, 0x00}; 

	    Buff.clear();

	    int result = UDiskGetData(upImgCmd,Buff,Buff_length, 1000);
	    Log.d(TAG, "UDiskGetData result="+result); 
	    if (result != 0)
	    {
	      return -101;
	    }
	    Buff.flip();
	    return result;
	  }
	
	//测试FF
	int UdiskTest(byte[] CMD,ByteBuffer Buff,int Receive_Length)
	  {
		Log.d(TAG, "UdiskTest()."); 
	    Buff.clear();
	    
	    int ret = -1;
		int i = 0;
		byte[] recvbuffer = new byte[Receive_Length];	
		byte[] di_CSW = new byte[13];
		byte[] do_CBW = new byte[]
				{	    (byte) 0x55, (byte) 0x53, (byte) 0x42, (byte) 0x43, // 固定值 0~3
						(byte) 0x28, (byte) 0xe8, (byte) 0x3e, (byte) 0xfe, // 4~7自定义,与返回的CSW中的值是一样的
						(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, // 8~11传输数据长度为512字节
						(byte) 0x80, 										// (12th 0x80-in)
						(byte) 0x00, 										// 13th LNU为0,则设为0
						(byte) 0x06, 										// 14th	命令长度 command length
						(byte) CMD[0], (byte) CMD[1], (byte) CMD[2], (byte)CMD[3], // READ	FORMAT CAPACITIES,后面的0x00皆被忽略
						(byte) CMD[4], (byte) CMD[5], (byte) 0x00, (byte) 0x00,	//				
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
						(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		do_CBW[8] = (byte) (Receive_Length & 0xFF);
		do_CBW[9] = (byte) (Receive_Length >> 8 & 0xFF);
		do_CBW[10] = (byte) (Receive_Length >> 16 & 0xFF);
		do_CBW[11] = (byte) (Receive_Length >> 24 & 0xFF);
	    
		ret = mConnection.bulkTransfer(mEndpointOut, do_CBW, do_CBW.length, 1000);
		
		if (ret != 31)
	    {
	      Log.e(TAG, "1..ret != 31,DO_CBW fail!\n");
	      return -301;
	    }
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, recvbuffer, recvbuffer.length, 1000);
		}
		Log.d(TAG, "mEndpointIn ret=" + ret);
		
		Buff.put(recvbuffer, 0, recvbuffer.length);	//	
		
		if ((this.mEndpointIn != null) && (this.mConnection != null))
		{
			ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, 1000);
		} 
		if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
		{
			Log.e(TAG, "2...DI_CSW fail!\n");
			return -302;
		}
		for (i = 4; i < 8; i++)
		{
			if (di_CSW[i] == do_CBW[i])
				continue;
			Log.e(TAG, "4...DI_CSW fail!\n");
			return -303;
		}
		Buff.flip();
		return 0;
	  }
	
	
	
}
