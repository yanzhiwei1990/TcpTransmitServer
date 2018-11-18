
public interface IMessageCallback {
	public void rece(byte[] data, int length);
	public void send(byte[] data, int length);
	public void disconnect();
}
