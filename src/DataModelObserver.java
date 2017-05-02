
/**
 * An interface which represent observers that need to be informed when the MapDataModel has changed in content(Loading a new File).
 * @author Jonathan Yin
 *
 */
public interface DataModelObserver {

	/**
	 * Reset should ask all observers to clear any data that may have used from the data model as it has changed
	 * since a new data model has been loaded.
	 */
	public void reset();
}
