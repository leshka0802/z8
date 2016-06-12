package org.zenframework.z8.server.ie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.zenframework.z8.server.engine.RmiIO;
import org.zenframework.z8.server.engine.RmiSerializable;
import org.zenframework.z8.server.types.guid;

public class TransportRoute implements RmiSerializable, Serializable {

	private static final long serialVersionUID = -2215330098958924695L;

	private guid routeId;
	private guid domainId;
	private String domain;
	private String protocol;
	private String address;

	private int priority = 0;
	private boolean active = true;

	public TransportRoute() {}
	
	public TransportRoute(guid routeId, guid domainId, String domain, String protocol, String address, int priority, boolean active) {
		this.routeId = routeId;
		this.domainId = domainId;
		this.domain = domain;
		this.protocol = protocol;
		this.address = address;
		this.priority = priority;
		this.active = active;
	}

	public guid getRouteId() {
		return routeId;
	}

	public guid getDomainId() {
		return domainId;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getTransportUrl() {
		return IeUtil.getUrl(getProtocol(), getAddress());
	}

	public String getDomain() {
		return domain;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getAddress() {
		return address;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		serialize(out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		deserialize(in);
	}

	@Override
	public void serialize(ObjectOutputStream out) throws IOException {
		RmiIO.writeGuid(out, routeId);
		RmiIO.writeGuid(out, domainId);
		RmiIO.writeString(out, domain);
		RmiIO.writeString(out, protocol);
		RmiIO.writeString(out, address);

		out.writeInt(priority);
		out.writeBoolean(active);
	}

	@Override
	public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
		routeId = RmiIO.readGuid(in);
		domainId = RmiIO.readGuid(in);
		domain = RmiIO.readString(in);
		protocol = RmiIO.readString(in);
		address = RmiIO.readString(in);

		priority = in.readInt();
		active = in.readBoolean();
	}
}