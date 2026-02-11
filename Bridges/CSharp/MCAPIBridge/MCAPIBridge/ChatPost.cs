namespace MCAPIBridge
{
    /// <summary>Chat Post</summary>
    public class ChatPost
    {
        public string Name { get; private set; }
        public string Message { get; private set; }

        public ChatPost(string name, string message)
        {
            Name = name;
            Message = message;
        }

        public override string ToString()
        {
            return string.Format("[{0}]: {1}", Name, Message);
        }
    }
}