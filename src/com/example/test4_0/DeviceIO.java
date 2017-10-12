package com.example.test4_0;

import java.nio.ByteBuffer;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.widget.Toast;


public class DeviceIO
{
	private static final String TAG = "DeviceIO";
	private static Toast mToast = null;
	private static final DeviceDatas Datas = new DeviceDatas();
	
	//打开设备
	public static UsbDevice OpenDevice(Context mContext, int VendorId, int ProductId)
	  {
	    UsbDevice mdevice = null;
	    mdevice = Datas.GetDevice(mContext, VendorId, ProductId);
	    boolean flag=Datas.makeConnection(mContext, mdevice);
	    if (!flag)
		{
			Log.d(TAG, "Sorry, GetUsbEndpoints failed!!!");
			return null;
		}
		Log.d(TAG, "GetUsbEndpoints Succeed!!!");
	    return mdevice;
	  }
	
	//关闭设备
	public static void CloseDevice(UsbDevice device)
	  {
		Log.d(TAG, "CloseDevice");
	    boolean isConnected = false;
	    isConnected = Datas.CheckConnection(device);
	    if (isConnected)
	    {
	      Datas.CloseConnection(device);
	      device = null;
	    }
	  }
	
	// 弹出通知
	public static void showToast(Context context, String msg, int duration)
	{
		if (mToast == null)
		{
			mToast = Toast.makeText(context, msg, duration);
		} else
		{
			mToast.setText(msg);
		}
		mToast.show();
	}
	
	//复位
	public static void DeviceReset(Context mContext)
	{
		try
		{
			Log.d(TAG, "DeviceReset");
			Datas.reset(mContext);
		} catch (Exception e)
		{
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//获取逻辑单元数
	public static int getMaxLnu()
	{
		int number=Datas.getMaxLnu();
		return number;
	}
	
	//核验设备
	public static int HS_Verfiy(UsbDevice device)
	{
		int nRet = -1;
		boolean isConnected = false;
		isConnected = Datas.CheckConnection(device);
		if (isConnected)
		{
			nRet = Datas.UDiskVerfiy();
			Log.d(TAG, "UDiskVerfiy return="+nRet+" 握手成功");
		}
		return nRet;
	}
	
	// 上传图像
	public static int UpImage(UsbDevice device, byte[] pImageData, int iImageLength)
	{
		int nRet = -1;
		boolean isConnected = false;
		isConnected = Datas.CheckConnection(device);
		if (isConnected)
		{
			// ByteBuffer Buffer = ByteBuffer.allocate(92160);
			
			//实际数据
			ByteBuffer Buffer = ByteBuffer.allocate(16384);//先用65536测试	64K
			nRet = Datas.UdiskUpImage(Buffer, Buffer.capacity());
			if (nRet == 0)//重点
			{	
				//复制给65536
				System.arraycopy(Buffer.array(), 0, pImageData, 0, Buffer.capacity());
			} else
			{
				Log.e("AS60xIO", "UdiskUpImage error： nRet=" + nRet);
			}
			Buffer.clear();

		} else
		{
			nRet = -3;
			Log.e(TAG, "CheckConnection error： nRet=" + nRet);
		}
		return nRet;
	}
	
	public static int Test(UsbDevice device, byte[] Data)
	{
		int nRet = -1;
		boolean isConnected = false;
		isConnected = Datas.CheckConnection(device);
		if (isConnected)
		{
			// ByteBuffer Buffer = ByteBuffer.allocate(92160);

			byte[] CMD =
			{ 0x01, 0x01, 0x00, 0x00, 0x24, 0x00 }; // ***@1设置指令
			// 实际数据
			ByteBuffer DataBuffer = ByteBuffer.allocate(256);// ***@3开辟16K	16384

			nRet = Datas.UdiskTest(CMD, DataBuffer,50);	//***@2设置一次接收长度

			if (nRet == 0)// 重点
			{
				// 复制给65536
				System.arraycopy(DataBuffer.array(), 0, Data, 0, 50);
			} else
			{
				Log.e("AS60xIO", "UdiskUpImage error： nRet=" + nRet);
			}
			DataBuffer.clear();

		} else
		{
			nRet = -3;
			Log.e(TAG, "CheckConnection error： nRet=" + nRet);
		}
		return nRet;
	}
	
	
}
