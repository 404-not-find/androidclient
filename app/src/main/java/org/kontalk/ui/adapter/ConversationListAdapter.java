/*
 * Kontalk Android client
 * Copyright (C) 2018 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.ui.adapter;

import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.jivesoftware.smack.util.StringUtils;
import org.kontalk.R;
import org.kontalk.data.Conversation;
import org.kontalk.ui.view.ConversationListItem;


public class ConversationListAdapter extends PagedListAdapter<Conversation, RecyclerView.ViewHolder> {

    private static final int ITEM_TYPE_ITEM = 0;
    private static final int ITEM_TYPE_FOOTER = 1;

    private static final DiffUtil.ItemCallback<Conversation> sDiffCallback = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(Conversation oldItem, Conversation newItem) {
            return oldItem.getThreadId() == newItem.getThreadId();
        }

        @Override
        public boolean areContentsTheSame(Conversation oldItem, Conversation newItem) {
            // include any attribute that might change the state of the UI
            return oldItem.getThreadId() == newItem.getThreadId() &&
                oldItem.getStatus() == newItem.getStatus() &&
                oldItem.getDate() == newItem.getDate() &&
                oldItem.isSticky() == newItem.isSticky() &&
                // this is for the count-only item
                oldItem.getMessageCount() == newItem.getMessageCount() &&
                oldItem.getUnreadCount() == newItem.getUnreadCount() &&
                oldItem.getRequestStatus() == newItem.getRequestStatus() &&
                StringUtils.nullSafeCharSequenceEquals(oldItem.getSubject(), newItem.getSubject()) &&
                StringUtils.nullSafeCharSequenceEquals(oldItem.getDraft(), newItem.getDraft());
        }
    };

    private final LayoutInflater mFactory;
    private SelectionTracker mSelectionTracker;
    private OnItemClickListener mItemListener;
    private OnFooterClickListener mFooterListener;

    public ConversationListAdapter(Context context) {
        super(sDiffCallback);
        mFactory = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    public void setSelectionTracker(SelectionTracker selectionTracker) {
        mSelectionTracker = selectionTracker;
    }

    public void setItemListener(OnItemClickListener itemListener) {
        mItemListener = itemListener;
    }

    public void setFooterListener(OnFooterClickListener footerListener) {
        mFooterListener = footerListener;
    }

    public int getRealItemCount() {
        if (getItemCount() > 0) {
            int count = getItemCount();
            Conversation conv = getItem(count - 1);
            return conv != null && conv.isCountOnly() ?
                count - 1 : count;
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getThreadId();
    }

    @Override
    public int getItemViewType(int position) {
        Conversation conv = getItem(position);
        if (conv != null && conv.isCountOnly()) {
            return ITEM_TYPE_FOOTER;
        }
        else {
            return ITEM_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_TYPE_FOOTER:
                return new ConversationFooterViewHolder(mFactory
                    .inflate(R.layout.conversation_list_footer, parent, false), mFooterListener);
            case ITEM_TYPE_ITEM:
            default:
                return new ConversationViewHolder((ConversationListItem) mFactory
                    .inflate(R.layout.conversation_list_item, parent, false), mItemListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ConversationViewHolder) {
            boolean selected = mSelectionTracker.isSelected(getItemId(position));
            ((ConversationViewHolder) holder).bindView(mFactory.getContext(), getItem(position), selected);
        }
        else if (holder instanceof ConversationFooterViewHolder) {
            Conversation archivedCount = getItem(position);
            ((ConversationFooterViewHolder) holder).bindView(mFactory.getContext(),
                archivedCount != null && archivedCount.getMessageCount() > 0 ?
                    archivedCount.getMessageCount() : null);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ConversationViewHolder) {
            ((ConversationViewHolder) holder).unbindView();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ConversationListItem item, int position);
    }

    public interface OnFooterClickListener {
        void onFooterClick();
    }

    public static final class ConversationItemDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView mListView;

        public ConversationItemDetailsLookup(RecyclerView listView) {
            mListView = listView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent event) {
            View view = mListView.findChildViewUnder(event.getX(), event.getY());
            if (view != null) {
                RecyclerView.ViewHolder holder = mListView.getChildViewHolder(view);
                if (holder instanceof ConversationViewHolder) {
                    return ((ConversationViewHolder) holder).getItemDetails();
                }
            }
            return null;
        }
    }

}
