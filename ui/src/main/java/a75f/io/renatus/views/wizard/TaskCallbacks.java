package a75f.io.renatus.views.wizard;

public interface TaskCallbacks {
	void onPreExecute();
	void onProgressUpdate(int percent);
	void onCancelled();
	void onPostExecute();
}
