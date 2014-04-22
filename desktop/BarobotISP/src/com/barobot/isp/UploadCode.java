package com.barobot.isp;

import com.barobot.common.IspSettings;
import com.barobot.common.constant.Methods;
import com.barobot.hardware.devices.BarobotConnector;
import com.barobot.hardware.devices.i2c.Carret;
import com.barobot.hardware.devices.i2c.Upanel;
import com.barobot.parser.Queue;
import com.barobot.parser.message.AsyncMessage;
import com.barobot.parser.message.Mainboard;
import com.barobot.parser.utils.Decoder;

public class UploadCode {
	public void prepareUpanels(Hardware hw ) {
		Queue q = hw.getQueue();
		hw.connectIfDisconnected();
		prepareUpanel2( Upanel.BACK, q, hw.barobot, hw );
		prepareUpanel2( Upanel.FRONT, q, hw.barobot, hw );
	}
	private void prepareUpanel2(final int row, Queue q, final BarobotConnector barobot, final Hardware hw ) {
		hw.barobot.i2c.clear();
		
		
		final int current_index		= 0;
		final Upanel firstInRow	= new Upanel();
		firstInRow.setRow(row);
		firstInRow.setIndex(row);
		firstInRow.setNumInRow(current_index);

		String command = "N" + row;
		q.add( new AsyncMessage( command, true ){			// has first upanel?
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith("" + Methods.METHOD_I2C_SLAVEMSG + ",")){		//	122,1,188,1
					int[] bytes = Decoder.decodeBytes(result);
					if(bytes[2] == Methods.METHOD_CHECK_NEXT  ){
						if(bytes[3] == 1 ){							// has next
							System.out.println("has next ROW "+row+"- OK");
							q.show("run");
							hw.barobot.i2c.add( firstInRow );
							Queue qq2	= UploadFirst( firstInRow, barobot, hw);
							q.addFirst(qq2);
						}else{
							System.out.println("ERROR: No device on ROW "+ row );
						}
						return true;
					}
				}
				return false;
			}
		});
	}

	private Queue UploadFirst(final Upanel current_dev, final BarobotConnector barobot, final Hardware hw) {
		final String hex_code = current_dev.getHexFile();
		Queue nq = new Queue();
		if( IspSettings.setFuseBits ){
			nq.add( new AsyncMessage( true ){
				@Override	
				public String getName() {
					return "isp upanel setFuseBits start" ;
				}
				@Override
				public Queue run(Mainboard dev, Queue queue) {
					Queue doAfter	= new Queue();
					current_dev.isp( doAfter );
					doAfter.add( new AsyncMessage( true ){
						@Override	
						public String getName() {
							return "isp upanel setFuseBits" ;
						}
						@Override
						public Queue run(Mainboard dev, Queue queue) {
							command = current_dev.setFuseBits( hw.comPort );
							Main.main.runCommand(command, hw);
							return null;
						}
					});
					return doAfter;
				}
			});	
		}
		if(IspSettings.setHex){
			nq.add( new AsyncMessage( true ){
				@Override	
				public String getName() {
					return "isp upanel setHex start" ;
				}
				@Override
				public Queue run(Mainboard dev, Queue queue) {
					Queue doAfter	= new Queue();
					current_dev.isp( doAfter );
					doAfter.add( new AsyncMessage( true ){
						@Override	
						public String getName() {
							return "isp upanel setHex" ;
						}
						@Override
						public Queue run(Mainboard dev, Queue queue) {
							command = current_dev.uploadCode( hex_code, hw.comPort );
							Main.main.runCommand(command, hw);
							return null;
						}
					});
					return doAfter;
				}
			});
		}
		final String resetCmd	= current_dev.getReset();
		nq.add( new AsyncMessage( resetCmd, true ){		// read address of the first upanel
			@Override
			public boolean onInput(String input, Mainboard dev, Queue mainQueue) {
				if( input.equals("R"+resetCmd) ){
					return true;		// its me, ignore message
				}
				return false;
			}
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith(""+ Methods.METHOD_DEVICE_FOUND +",")){		//	112,18,19,1
					int[] bytes = Decoder.decodeBytes(result);	// HELLO, ADDRESS, TYPE, VERSION
					current_dev.setAddress(bytes[1]);
					System.out.println("+Upanel " + current_dev.getNumInRow() + " ma adres " + current_dev.getAddress());
					Queue qq2	= checkHasNext( hw, barobot, current_dev, hex_code );	
					current_dev.setLed( qq2, "22", 255 );
					q.addFirst(qq2);
					return true;
				}
				return false;
			}
		});
		return nq;
	}

	private Queue checkHasNext( final Hardware hw, final BarobotConnector barobot, final Upanel current_dev, String upanel_code ) {
		Queue nq = new Queue();	
		String command = "n" + current_dev.getAddress();
		nq.add( new AsyncMessage( command, true ){			// has first upanel?
			@Override
			public boolean isRet(String result, Queue q) {
				if(result.startsWith("" + Methods.METHOD_I2C_SLAVEMSG + ",")){		//	122,1,188,1
					int[] bytes = Decoder.decodeBytes(result);
					if(bytes[2] == Methods.METHOD_CHECK_NEXT  ){
						if(bytes[3] == 1 ){							// has next
							System.out.println("has next ROW "+current_dev.getAddress()+"- OK");
							Upanel next_device	= new Upanel();
							next_device.setRow( current_dev.getRow() );
							next_device.isResetedBy( current_dev );
							next_device.setNumInRow( current_dev.getBottleNum()+1 );
							hw.barobot.i2c.add( next_device );
							Queue qq2	= UploadFirst( next_device, barobot, hw);
							q.addFirst(qq2);
						}else if( current_dev.getNumInRow() >= 5 ){	// all found
						}else{
							System.out.println("ERROR: No device after "+ current_dev.getAddress() );
						}
						return true;
					}
				}
				return false;
			}
		});
		return nq;
	}
	public void prepareCarret(final Hardware hw) {
		Queue q = hw.getQueue();
		hw.connectIfDisconnected();
		q.add( "\n", false );
		q.add( "\n", false );
		q.add("PING", "PONG");
		final Carret current_dev	= hw.barobot.i2c.carret;
		final String carret_code = current_dev.getHexFile();
		if( IspSettings.setFuseBits){
			q.add( new AsyncMessage( true ){
				@Override
				public String getName() {
					return "isp carret setFuseBits" ;
				}
				@Override
				public Queue run(Mainboard dev, Queue queue) {
					Queue doAfter	= new Queue();
					current_dev.isp( doAfter );
					doAfter.add( new AsyncMessage( true ){
						@Override	
						public String getName() {
							return "isp carret setFuseBits" ;
						}
						@Override
						public Queue run(Mainboard dev, Queue queue) {
							command = current_dev.setFuseBits( hw.comPort );
							Main.main.runCommand(command, hw);
							return null;
						}
					});
					return doAfter;
				}
			});
		}
		if(IspSettings.setHex){
			q.add( new AsyncMessage( true ){
				@Override	
				public String getName() {
					return "isp carret setHex" ;
				}
				@Override
				public Queue run(Mainboard dev, Queue queue) {
					Queue doAfter	= new Queue();
					current_dev.isp( doAfter );

					doAfter.add( new AsyncMessage( true ){
						@Override	
						public String getName() {
							return "isp carret setHex" ;
						}
						@Override
						public Queue run(Mainboard dev, Queue queue) {
							command = current_dev.uploadCode( carret_code, hw.comPort );
							Main.main.runCommand(command, hw);
							return null;
						}
					});
					return doAfter;
				}
			});
		}
	}
	public void clearUpanel(Hardware hw) {
		Upanel[] list = hw.barobot.i2c.getUpanels();
		while(list.length > 0 ){
			boolean found = false;
			/*
			for (Upanel u : hw.barobot.i2c.list){
				if(u.have_reset_to == null ){
					 System.out.println("Rozpoczynam id " + u.getAddress() );
					 hw.barobot.i2c.list.remove(u);
					 u.can_reset_me_dev.have_reset_to = null;
					 found = true;
					 break;
				}
			}*/
			if(!found){
				System.out.println("Brak w�z��w ko�cowych" );
				break;
			}
		}
		System.out.println("Lista pusta" );
	}
}
