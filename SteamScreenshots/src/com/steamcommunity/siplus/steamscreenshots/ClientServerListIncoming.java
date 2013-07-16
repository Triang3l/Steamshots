package com.steamcommunity.siplus.steamscreenshots;

import java.util.Iterator;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientServerListProto;
import com.steamcommunity.siplus.steamscreenshots.proto.IncomingProtos.ClientServerListProto.ClientServerListProtoServer;

public class ClientServerListIncoming extends Incoming {
	static final int MESSAGE = 880;

	String mUFSAddress;
	int mUFSPort;

	ClientServerListIncoming(IncomingData data) throws IncomingException {
		super(data);
	}

	@Override
	void fromProtobuf(byte[] data) throws IncomingException {
		ClientServerListProto proto;
		try {
			proto = ClientServerListProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IncomingException();
		}
		long ip;
		Iterator<ClientServerListProtoServer> iterator;
		ClientServerListProtoServer server;
		List<ClientServerListProtoServer> servers = proto.getServersList();
		for (iterator = servers.iterator(); iterator.hasNext(); ) {
			server = iterator.next();
			if ((server.getServerType() != 21) || !(server.hasServerIP() && server.hasServerPort())) {
				continue;
			}
			ip = server.getServerIP();
			if (ip < 0L) {
				ip += 0x100000000L;
			}
			mUFSAddress = String.format("%d.%d.%d.%d", ip >> 24L, (ip >> 16L) & 255L, (ip >> 8L) & 255L, ip & 255L);
			mUFSPort = server.getServerPort();
			return;
		}
	}

	@Override
	void fromRaw(byte[] data) throws IncomingException {
		throw new IncomingException();
	}
}
